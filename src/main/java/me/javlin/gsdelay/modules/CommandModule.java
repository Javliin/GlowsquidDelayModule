package me.javlin.gsdelay.modules;

import me.javlin.glowsquid.mc.chat.ChatComponent;
import me.javlin.glowsquid.mc.chat.Color;
import me.javlin.glowsquid.mc.chat.Style;
import me.javlin.glowsquid.network.packet.event.PacketEvent;
import me.javlin.glowsquid.network.packet.event.PacketSendEvent;
import me.javlin.glowsquid.network.packet.impl.play.inbound.PacketServerChat;
import me.javlin.glowsquid.network.packet.impl.play.outbound.PacketClientChat;
import me.javlin.glowsquid.network.proxy.module.Module;
import me.javlin.glowsquid.network.util.UtilDataType;
import me.javlin.gsdelay.DelayedType;

import java.util.HashMap;
import java.util.Map;

public class CommandModule extends Module {
    private static final Map<String, DelayedType> COMMANDS = new HashMap<String, DelayedType>(){{
        put("erange", DelayedType.ENEMY_RANGE);
        put("edelay", DelayedType.ENEMY_DELAY);
        //put("range", DelayedType.PLAYER_RANGE);
        put("tps", DelayedType.TELEPORT_SMOOTHING);
    }};

    @PacketEvent
    public void onChatPacket(PacketSendEvent<PacketClientChat> event) {
        PacketClientChat chat = event.getPacket();

        String returnMessage = "";
        String message = chat.getMessage().toLowerCase();
        String[] splitMessage = message.split(" ");

        if (splitMessage.length < 1 || !splitMessage[0].startsWith("!")) {
            return;
        }

        splitMessage[0] = splitMessage[0].substring(1);
        String command = splitMessage[0].toLowerCase();

        if (command.equals("help")) {
            returnMessage = "Enemy range delay (erange): " + DelayedType.ENEMY_RANGE.getDelay() +
                    "\nEnemy general delay (edelay): " + DelayedType.ENEMY_DELAY.getDelay() +
                    //"\nPlayer range delay (range): " + DelayedType.PLAYER_RANGE.getDelay() +
                    "\nTeleport smoothing delay (tps): " + DelayedType.TELEPORT_SMOOTHING.getDelay();
        } else if (splitMessage.length > 1 && UtilDataType.isNumeric(splitMessage[1])){
            int setting = Integer.parseInt(splitMessage[1]);
            DelayedType type = COMMANDS.get(command);

            if (type != null) {
                type.setDelay(setting);
                returnMessage = String.format("%s set to: %d", type.getDescription(), setting);
            }
        }

        if (!returnMessage.equals("")) {
            manager.getSession().queueInbound(new PacketServerChat(
                    new ChatComponent(returnMessage, Color.GREEN, Style.BOLD).build(),
                    manager.getSession().is18()
            ));
            event.setCancelled(true);
        }
    }
}
