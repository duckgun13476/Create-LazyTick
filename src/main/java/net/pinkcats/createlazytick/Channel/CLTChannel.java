package net.pinkcats.createlazytick.Channel;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

@EventBusSubscriber(modid = MODID)
public class CLTChannel {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID)
                .versioned(PROTOCOL_VERSION);

        registrar.playToServer(
                ClockSyncPacket.TYPE,
                ClockSyncPacket.STREAM_CODEC,
                ClockSyncPacket::handle
        );
    }

    public static <MSG extends CustomPacketPayload> void sendToServer(MSG message) {
        PacketDistributor.sendToServer(message);
    }
}
