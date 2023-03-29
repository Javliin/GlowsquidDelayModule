package me.javlin.gsdelay.modules;

import me.javlin.glowsquid.mc.Player;
import me.javlin.glowsquid.network.packet.event.PacketEvent;
import me.javlin.glowsquid.network.packet.event.PacketSendEvent;
import me.javlin.glowsquid.network.packet.impl.play.outbound.PacketPlayerMove;
import me.javlin.glowsquid.network.proxy.module.Module;
import me.javlin.gsdelay.DelayedPacket;
import me.javlin.gsdelay.DelayedType;

import java.util.List;
import java.util.Map;

public class ClientMovementDelayModule extends Module {
    private static final Map<DelayedType, List<DelayedPacket>> delayedPacketQueue = DelayedPacket.getDelayedPacketQueue();

    private static final float RANGE = 3.9F;
    private static final float MAX_DIFF = 0.7F;

    @PacketEvent
    public void onPlayerMove(PacketSendEvent<PacketPlayerMove> event) {
        PacketPlayerMove playerMove = event.getPacket();

        processMove(new PacketPlayerMove(
                playerMove.getX(),
                playerMove.getY(),
                playerMove.getZ(),
                playerMove.isOnGround(),
                false
        ), event);
    }

    private void processMove(PacketPlayerMove playerMove, PacketSendEvent<PacketPlayerMove> event) {
        Player potentialPlayer = new Player(playerMove.getX(), playerMove.getY(), playerMove.getZ());

        /*if (DelayedType.PLAYER_RANGE.getDelay() != 0) {
            synchronized (delayedPacketQueue) {
                List<DelayedPacket> firstDelayed = delayedPacketQueue.get(DelayedType.PLAYER_RANGE);

                if (!firstDelayed.isEmpty() || checkPlayerDelayed(potentialPlayer)) {
                    DelayedPacket.delayQueue(DelayedType.PLAYER_RANGE, player.getEntityId(), playerMove);
                    event.setCancelled(true);
                }
            }
        }*/

        player.setPos(potentialPlayer.getX(), potentialPlayer.getY(), potentialPlayer.getZ());
    }

    /*private boolean checkPlayerDelayed(Player potentialPlayer) {
        double distance, potentialDistance;

        for (Player player : playerList.values()) {
            distance = this.player.distanceTo(player);
            potentialDistance = potentialPlayer.distanceTo(player);

            if (DelayedPacket.shouldBeDelayed(RANGE, MAX_DIFF, potentialDistance, distance)) {
                return true;
            }
        }

        return false;
    }*/
}
