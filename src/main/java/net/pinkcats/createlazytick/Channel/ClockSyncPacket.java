package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClockSyncPacket {

    private final BlockPos pos;
    private final String dimension;
    private final int extraData;

    public static List<ClientData> PacketCache = new ArrayList<>();


    public ClockSyncPacket(int extraData , String dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
        this.extraData = extraData;
    }

    public ClockSyncPacket(FriendlyByteBuf buf) {
        dimension = buf.readUtf();
        pos = buf.readBlockPos();
        extraData = buf.readInt();

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeInt(extraData);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ClientData data = new ClientData(extraData,dimension,pos);
        System.out.println("handle:" + data);

        if (PacketCache.size() > 80) {
            PacketCache.clear();
        }

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
                ", extraData="  + extraData +
                '}';
    }
}
