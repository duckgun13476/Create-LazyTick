package net.pinkcats.NutUI.menu.Connect;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.architect.data.EntryListPacket;
import net.pinkcats.NutUI.menu.architect.data.SharedData;

import java.util.HashMap;
import java.util.Map;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;
import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;

@EventBusSubscriber(modid = MODID)
public class Channel {

    private static final String PROTOCOL_VERSION = "2";
    private static final boolean SYNC_DEBUG_LOG = false;
    private static final int DEFAULT_SYNC_INTERVAL_TICKS = 20;
    //private static boolean PACKETS_REGISTERED = false;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID).versioned(PROTOCOL_VERSION);

        // 注册 EntryListPacket (服务器发往客户端)
        registrar.playToClient(
                EntryListPacket.TYPE,
                EntryListPacket.STREAM_CODEC,
                EntryListPacket::handle
        );

        // 注册 DataPacket (双向通信: 服务器 <-> 客户端)
        registrar.playBidirectional(
                DataPacket.TYPE,
                DataPacket.STREAM_CODEC,
                DataPacket::handle
        );

        // 注册 MenuActionPacket (客户端发往服务器)
        registrar.playToServer(
                MenuActionPacket.TYPE,
                MenuActionPacket.STREAM_CODEC,
                MenuActionPacket::handle
        );
    }

    public static <MSG extends CustomPacketPayload> void setMsgToPlayer(MSG message, ServerPlayer player) {
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        PacketDistributor.sendToPlayer(player, message);
    }

    public static <MSG extends CustomPacketPayload> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        PacketDistributor.sendToPlayer(player, message);
    }

    public static <MSG extends CustomPacketPayload> void sendToServer(MSG message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendActionToServer(MenuActionPacket packet) {
        PacketDistributor.sendToServer(packet);
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
        if (player == null || menu == null) {
            return;
        }
        syncMenuDataToPlayer(player, currentDimensionId(player), menu.buildAutoSyncVariables());
    }

    public static int currentDimensionId(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return player.level().dimension().location().hashCode();
    }


    @EventBusSubscriber(modid = MODID)
    public static class GameEvents {
        @SubscribeEvent
        public static void autoSyncMenuData(PlayerTickEvent.Post event) {
            // [迁移] 实体获取方式变更
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            if (!(player.containerMenu instanceof NutKineticMenu.NutItemMenu menu)) {
                return;
            }

            if (player.level().getGameTime() % DEFAULT_SYNC_INTERVAL_TICKS != 0) {
                return;
            }

            syncMenuDataToPlayer(player, currentDimensionId(player), menu.buildAutoSyncVariables());
        }
    }
}
