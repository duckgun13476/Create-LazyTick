package net.pinkcats.createlazytick.Channel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

@Environment(EnvType.CLIENT)
public final class ClientClockNetwork {
    private ClientClockNetwork() {}

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(CLTChannel.CLOCK_STATE, (client, handler, buf, responseSender) -> {
            LazyTickStatePacket packet = new LazyTickStatePacket(buf);
            client.execute(() -> ClientClockStateHandler.accept(packet.dimension(), packet.pos(), packet.state()));
        });
    }

    static void send(ClockSyncPacket packet) {
        var buffer = PacketByteBufs.create();
        packet.encode(buffer);
        ClientPlayNetworking.send(CLTChannel.CLOCK_SYNC, buffer);
    }
}
