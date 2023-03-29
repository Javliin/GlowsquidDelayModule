package me.javlin.gsdelay.modules;

import me.javlin.glowsquid.mc.Player;
import me.javlin.glowsquid.network.packet.PacketInfo;
import me.javlin.glowsquid.network.packet.event.PacketEvent;
import me.javlin.glowsquid.network.packet.event.PacketSendEvent;
import me.javlin.glowsquid.network.packet.impl.play.inbound.PacketEntityLookAndMove;
import me.javlin.glowsquid.network.packet.impl.play.inbound.PacketEntityMove;
import me.javlin.glowsquid.network.packet.impl.play.inbound.PacketEntityTeleport;
import me.javlin.glowsquid.network.proxy.module.Module;
import me.javlin.gsdelay.DelayedPacket;
import me.javlin.gsdelay.DelayedType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServerMovementDelayModule extends Module {
    private static final Map<DelayedType, List<DelayedPacket>> delayedPacketQueue = DelayedPacket.getDelayedPacketQueue();
    
    private static final float RANGED_RANGE = 3.7F;
    private static final float RANGED_MAX_DIFF = 0.85F;

    private static final float GENERAL_RANGE = 4F;
    private static final float GENERAL_MAX_DIFF = 0.6F;
    private static final float GENERAL_MIN_DIFF = 0.2F;

    private boolean is18;
    
    @Override
    public void onEnable() {
        manager.getSession().scheduleRepeatingTask(this, () -> {
            synchronized (delayedPacketQueue) {
                delayedPacketQueue.forEach((type, packets) -> {
                    Iterator<DelayedPacket> iterator = packets.iterator();
                    DelayedPacket packet;

                    while (iterator.hasNext()) {
                        packet = iterator.next();

                        if (System.currentTimeMillis() >= packet.getSendTime()) {
                            if (type.getDirection() == PacketInfo.PacketDirection.OUTBOUND) {
                                manager.getSession().queueOutbound(packet.getPacket());
                            } else {
                                manager.getSession().queueInbound(packet.getPacket());
                            }

                            iterator.remove();
                        }
                    }
                });
            }
        });

        is18 = manager.getSession().is18();
    }

    @PacketEvent
    public void onEntityTeleport(PacketSendEvent<PacketEntityTeleport> event) {
        PacketEntityTeleport packet = event.getPacket();

        int entityId = packet.getEntityId();

        if(playerList.containsKey(entityId)) {
            Long delayedPlayer = DelayedPacket.getPlayer(entityId);

            int x = packet.getX();
            int y = packet.getY();
            int z = packet.getZ();

            // These are often sent even when the player moves less than 4 blocks, so delay the
            // movement more if there is an existing delay instead of cancelling the delay
            if(delayedPlayer != null) {
                Player player = playerList.get(entityId);

                synchronized (delayedPacketQueue) {
                    if (DelayedType.TELEPORT_SMOOTHING.getDelay() != 0) {

                        // Positions of players in the list are always updated regardless of delay, so these deltas should be accurate
                        double xDiff = x - (player.getX() * 32D);
                        double yDiff = y - (player.getY() * 32D);
                        double zDiff = z - (player.getZ() * 32D);

                        // If the movement is greater than four blocks in any direction, cancel all delayed movement packets
                        if (xDiff > 128 || yDiff > 128 || zDiff > 128 || xDiff < -128 || yDiff < -128 || zDiff < -128) {
                            playerList.get(entityId).setPos(x / 32D, y / 32D, z / 32D);
                            cancelDelay(entityId);
                            return;
                        }

                        // Convert the teleport packet to a relative movement packet and split it
                        byte dx = (byte) xDiff;
                        byte dy = (byte) yDiff;
                        byte dz = (byte) zDiff;

                        byte yaw = packet.getYaw();
                        byte pitch = packet.getPitch();

                        boolean onGround = packet.isOnGround();

                        PacketEntityLookAndMove packetMove = new PacketEntityLookAndMove(entityId, dx, dy, dz, yaw, pitch, onGround, is18);

                        DelayedPacket.splitDelayQueue(
                                manager.getSession(),
                                DelayedType.TELEPORT_SMOOTHING,
                                delayedPlayer,
                                packetMove
                        );

                        event.setCancelled(true);
                    } else {
                        playerList.get(entityId).setPos(x / 32D, y / 32D, z / 32D);
                        cancelDelay(entityId);
                    }
                }
            }

            playerList.get(entityId).setPos(x / 32D, y / 32D, z / 32D);
        }
    }

    @PacketEvent
    public void onEntityMove(PacketSendEvent<PacketEntityMove> event) {
        PacketEntityMove entityMove = event.getPacket();

        int entityId = entityMove.getEntityId();

        if (playerList.containsKey(entityId)) {
            Player player = playerList.get(entityId);

            Player potentialPlayer = new Player(
                    player.getX() + (entityMove.getDx() / 32D),
                    player.getY() + (entityMove.getDy() / 32D),
                    player.getZ() + (entityMove.getDz() / 32D)
            );

            double newDistance = this.player.distanceTo(potentialPlayer);
            double originalDistance = this.player.distanceTo(player);

            player.setPos(potentialPlayer.getX(),
                    potentialPlayer.getY(),
                    potentialPlayer.getZ());

            synchronized (delayedPacketQueue) {
                List<DelayedPacket> teleportSmoothingDelayed = delayedPacketQueue.get(DelayedType.TELEPORT_SMOOTHING);
                List<DelayedPacket> enemyRangeDelayed = delayedPacketQueue.get(DelayedType.ENEMY_RANGE);
                List<DelayedPacket> enemyGeneralDelayed = delayedPacketQueue.get(DelayedType.ENEMY_DELAY);

                DelayedType type = null;

                // Only handle one type of delay at a time
                if (!teleportSmoothingDelayed.isEmpty()) {
                    type = DelayedType.TELEPORT_SMOOTHING;
                } else if (!enemyRangeDelayed.isEmpty() || checkRangeDelayed(entityId, newDistance, originalDistance)) {
                    type = DelayedType.ENEMY_RANGE;
                } else if (!enemyGeneralDelayed.isEmpty() || checkGeneralDelayed(newDistance, originalDistance)) {
                    type = DelayedType.ENEMY_DELAY;
                }

                if(type != null) {
                    DelayedPacket.splitDelayQueue(manager.getSession(), type, entityMove);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void cancelDelay(int entityId) {
        delayedPacketQueue.forEach((type, queue) -> queue.clear());
        DelayedPacket.removePlayer(entityId);
    }

    private boolean checkRangeDelayed(int entityId, double newDistance, double originalDistance) {
        return DelayedType.ENEMY_RANGE.getDelay() > 0
                && recentlyAttackedPlayers.containsKey(entityId)
                && DelayedPacket.shouldBeDelayed(RANGED_RANGE, RANGED_MAX_DIFF, originalDistance, newDistance);
    }

    private boolean checkGeneralDelayed(double newDistance, double originalDistance) {
       return DelayedType.ENEMY_DELAY.getDelay() > 0
               && newDistance > originalDistance
               && newDistance <= GENERAL_RANGE
               && newDistance - originalDistance <= GENERAL_MAX_DIFF
               && newDistance - originalDistance >= GENERAL_MIN_DIFF;
    }
}
