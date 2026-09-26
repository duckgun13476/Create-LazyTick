package net.pinkcats.NutUI.menu.Connect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.architect.data.EntryListPacket;
import net.pinkcats.NutUI.menu.architect.data.SharedData;

import java.util.HashMap;
import java.util.Map;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.BuildDefine;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;
import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Channel {

    private static final String PROTOCOL_VERSION = "2";
    private static final boolean SYNC_DEBUG_LOG = false;
    private static final int DEFAULT_SYNC_INTERVAL_TICKS = 20;
    private static boolean PACKETS_REGISTERED = false;

    public static final SimpleChannel INSTANCE_TO_SERVER = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID, "nutui_sync_to_server"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register_to_server() {
        ensurePacketsRegistered();
    }

    public static final SimpleChannel MSG_TO_CLIENT = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID, "nutui_msg_to_client"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register_msg_to_player() {
        ensurePacketsRegistered();
    }

    public static final SimpleChannel INSTANCE_TO_CLIENT = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID, "nutui_sync_to_client"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    static {
        ensurePacketsRegistered();
    }

    public static void register_to_player() {
        ensurePacketsRegistered();
    }

    public static <MSG> void setMsgToPlayer(MSG message, ServerPlayer player) {
        ensurePacketsRegistered();
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        MSG_TO_CLIENT.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        ensurePacketsRegistered();
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        INSTANCE_TO_CLIENT.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        ensurePacketsRegistered();
        INSTANCE_TO_SERVER.sendToServer(message);
    }

    public static void sendActionToServer(MenuActionPacket packet) {
        ensurePacketsRegistered();
        INSTANCE_TO_SERVER.sendToServer(packet);
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

    @SubscribeEvent
    public static void autoSyncMenuData(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ensurePacketsRegistered();

        if (!(player.containerMenu instanceof NutKineticMenu.NutItemMenu menu)) {
            return;
        }

        if (player.level().getGameTime() % DEFAULT_SYNC_INTERVAL_TICKS != 0) {
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

    public static synchronized void ensurePacketsRegistered() {
        if (PACKETS_REGISTERED) {
            return;
        }

        INSTANCE_TO_SERVER.messageBuilder(DataPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .decoder(DataPacket::new)
                .encoder(DataPacket::encode)
                .consumerMainThread(DataPacket::handle)
                .add();

        INSTANCE_TO_SERVER.messageBuilder(MenuActionPacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .decoder(MenuActionPacket::new)
                .encoder(MenuActionPacket::encode)
                .consumerMainThread(MenuActionPacket::handle)
                .add();

        MSG_TO_CLIENT.messageBuilder(EntryListPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(EntryListPacket::new)
                .encoder(EntryListPacket::encode)
                .consumerMainThread(EntryListPacket::handle)
                .add();

        INSTANCE_TO_CLIENT.messageBuilder(DataPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(DataPacket::new)
                .encoder(DataPacket::encode)
                .consumerMainThread(DataPacket::handle)
                .add();

        PACKETS_REGISTERED = true;
        // LOGGER.info("[NutUI Sync] Channel packets registered.");
    }
}
