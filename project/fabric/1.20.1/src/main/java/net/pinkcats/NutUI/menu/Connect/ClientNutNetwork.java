package net.pinkcats.NutUI.menu.Connect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.pinkcats.NutUI.menu.architect.data.EntryListPacket;

@Environment(EnvType.CLIENT)
public final class ClientNutNetwork {
    private ClientNutNetwork() {}

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(Channel.ENTRY_TO_CLIENT, (client, handler, buf, responseSender) -> {
            EntryListPacket packet = new EntryListPacket(buf);
            client.execute(packet::handleClient);
        });
        ClientPlayNetworking.registerGlobalReceiver(Channel.DATA_TO_CLIENT, (client, handler, buf, responseSender) -> {
            DataPacket packet = new DataPacket(buf);
            client.execute(packet::handleClient);
        });
    }

    static void sendData(DataPacket packet) {
        var buffer = PacketByteBufs.create();
        packet.encode(buffer);
        ClientPlayNetworking.send(Channel.DATA_TO_SERVER, buffer);
    }

    static void sendAction(MenuActionPacket packet) {
        var buffer = PacketByteBufs.create();
        packet.encode(buffer);
        ClientPlayNetworking.send(Channel.ACTION_TO_SERVER, buffer);
    }
}
