package net.pinkcats.createlazytick.Channel;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

@EventBusSubscriber(modid = MODID)
public class CLTChannel {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("[CreateLazyTick][Network] registering CLT payload handlers");
        final PayloadRegistrar registrar = event.registrar(MODID)
                .versioned(PROTOCOL_VERSION);

        registrar.playToServer(
                ClockSyncPacket.TYPE,
                ClockSyncPacket.STREAM_CODEC,
                ClockSyncPacket::handle
        );

        registrar.playToClient(
                LazyTickStatePacket.TYPE,
                LazyTickStatePacket.STREAM_CODEC,
                LazyTickStatePacket::handle
        );
    }

    public static <MSG extends CustomPacketPayload> void sendToServer(MSG message) {
        PacketDistributor.sendToServer(message);
    }

    public static <MSG extends CustomPacketPayload> void sendToPlayer(MSG message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
