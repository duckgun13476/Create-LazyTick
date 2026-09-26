package net.pinkcats.createlazytick.Channel;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public final class CLTChannel {
    public static final ResourceLocation CLOCK_SYNC = new ResourceLocation(MODID, "dimension_to_server");

    private CLTChannel() {}

    public static void register_to_server() {
        ServerPlayNetworking.registerGlobalReceiver(CLOCK_SYNC, (server, player, handler, buf, responseSender) -> {
            ClockSyncPacket packet = new ClockSyncPacket(buf);
            server.execute(() -> packet.handle(player));
        });
    }

    public static void sendToServer(ClockSyncPacket packet) {
        ClientClockNetwork.send(packet);
    }
}
