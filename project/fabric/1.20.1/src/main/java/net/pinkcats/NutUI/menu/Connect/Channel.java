package net.pinkcats.NutUI.menu.Connect;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.architect.data.EntryListPacket;
import net.pinkcats.NutUI.menu.architect.data.SharedData;

import java.util.HashMap;
import java.util.Map;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.BuildDefine;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;
import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;

public final class Channel {
    public static final ResourceLocation DATA_TO_SERVER = BuildDefine(MODID, "nutui_sync_to_server");
    public static final ResourceLocation ACTION_TO_SERVER = BuildDefine(MODID, "nutui_action_to_server");
    public static final ResourceLocation ENTRY_TO_CLIENT = BuildDefine(MODID, "nutui_msg_to_client");
    public static final ResourceLocation DATA_TO_CLIENT = BuildDefine(MODID, "nutui_sync_to_client");
    private static final boolean SYNC_DEBUG_LOG = false;
    private static final int DEFAULT_SYNC_INTERVAL_TICKS = 20;
    private static boolean packetsRegistered;

    private Channel() {}

    public static void register_to_server() { ensurePacketsRegistered(); }
    public static void register_msg_to_player() { ensurePacketsRegistered(); }
    public static void register_to_player() { ensurePacketsRegistered(); }

    public static void setMsgToPlayer(EntryListPacket message, ServerPlayer player) {
        if (player == null) return;
        var buffer = PacketByteBufs.create();
        message.encode(buffer);
        ServerPlayNetworking.send(player, ENTRY_TO_CLIENT, buffer);
    }

    public static void sendToPlayer(DataPacket message, ServerPlayer player) {
        if (player == null) return;
        var buffer = PacketByteBufs.create();
        message.encode(buffer);
        ServerPlayNetworking.send(player, DATA_TO_CLIENT, buffer);
    }

    public static void sendToServer(DataPacket message) {
        ClientNutNetwork.sendData(message);
    }

    public static void sendActionToServer(MenuActionPacket packet) {
        ClientNutNetwork.sendAction(packet);
    }

    public static void syncMenuDataToPlayer(ServerPlayer player, int dimension, Map<String, ?> variables) {
        Map<String, Object> payload = variables == null ? new HashMap<>() : new HashMap<>(variables);
        payload.putIfAbsent("dimension", dimension);
        payload.put(DataPacket.DEMO_SYNC_KEY, true);
        payload.put(DataPacket.DEMO_SYNC_VERSION_KEY, "v1");
        payload.put(DataPacket.DEMO_SYNC_TICK_KEY, player.level().getGameTime());
        if (SYNC_DEBUG_LOG) {
            LOGGER.info("[NutUI Sync][SEND][Server->Client] player={} dimension={} keys={} payload={}",
                    player.getGameProfile().getName(), dimension, payload.keySet(), payload);
        }
        sendToPlayer(new DataPacket(dimension, SharedData.getCoordinatesList(), payload), player);
    }

    public static void syncOpenedMenuNow(ServerPlayer player, NutKineticMenu.NutItemMenu menu) {
        if (player != null && menu != null) {
            syncMenuDataToPlayer(player, currentDimensionId(player), menu.buildAutoSyncVariables());
        }
    }

    public static int currentDimensionId(ServerPlayer player) {
        return player == null ? 0 : player.level().dimension().location().hashCode();
    }

    public static synchronized void ensurePacketsRegistered() {
        if (packetsRegistered) return;
        ServerPlayNetworking.registerGlobalReceiver(DATA_TO_SERVER, (server, player, handler, buf, responseSender) -> {
            new DataPacket(buf); // Preserve the existing server-side no-op for this packet type.
        });
        ServerPlayNetworking.registerGlobalReceiver(ACTION_TO_SERVER, (server, player, handler, buf, responseSender) -> {
            MenuActionPacket packet = new MenuActionPacket(buf);
            server.execute(() -> packet.handle(player));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.overworld().getGameTime() % DEFAULT_SYNC_INTERVAL_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof NutKineticMenu.NutItemMenu menu) {
                    syncMenuDataToPlayer(player, currentDimensionId(player), menu.buildAutoSyncVariables());
                }
            }
        });
        packetsRegistered = true;
    }
}
