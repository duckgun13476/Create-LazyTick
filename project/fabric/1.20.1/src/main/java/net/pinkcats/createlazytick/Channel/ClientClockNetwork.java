package net.pinkcats.createlazytick.Channel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

@Environment(EnvType.CLIENT)
final class ClientClockNetwork {
    private ClientClockNetwork() {}

    static void send(ClockSyncPacket packet) {
        var buffer = PacketByteBufs.create();
        packet.encode(buffer);
        ClientPlayNetworking.send(CLTChannel.CLOCK_SYNC, buffer);
    }
}
