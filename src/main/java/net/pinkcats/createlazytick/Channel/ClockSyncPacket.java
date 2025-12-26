package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClockSyncPacket {

    private final BlockPos pos;
    private final int dimension;
    private final String Player;

    public static List<ClientData> PacketCache = new ArrayList<>();


    public ClockSyncPacket(String Player , int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
        this.Player = Player;
    }

    public ClockSyncPacket(FriendlyByteBuf buf) {
        dimension = buf.readInt();
        pos = buf.readBlockPos();
        Player = buf.readUtf();

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeBlockPos(pos);
        buf.writeUtf(Player);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ClientData data = new ClientData(Player,dimension,pos);
        System.out.println(data);
        for (ClientData existingData : PacketCache) {
            System.out.println("awa "+existingData.toString());
            if (data.isSimilar(existingData))
                    return;
        }
        PacketCache.add(data);
        ctx.setPacketHandled(true);
    }


    @Override
    public String toString() {
        return "Packet{" +
                "dimension=" + dimension +
                ", pos="  + pos +
                ", Player="  + Player +
                '}';
    }
}
