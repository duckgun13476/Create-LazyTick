package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClockSyncPacket {

    private final BlockPos pos;
    private final String dimension;
    private final int extraData;
    private final boolean isQuery;

    public static List<ClientData> PacketCache = new ArrayList<>();

    // 构造函数 1: 纯查询 (Tooltip 用)
    public ClockSyncPacket(BlockPos pos) {
        this.pos = pos;
        this.dimension = "";
        this.extraData = 0;
        this.isQuery = true; // 标记为查询
    }

    // [保留] 构造函数 2: 旧逻辑兼容 (设置用)
    public ClockSyncPacket(int extraData , String dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
        this.extraData = extraData;
        this.isQuery = false; // 标记为设置
    }

    public ClockSyncPacket(FriendlyByteBuf buf) {
        dimension = buf.readUtf();
        pos = buf.readBlockPos();
        extraData = buf.readInt();
        isQuery = buf.readBoolean(); // [新增] 读取标记
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeInt(extraData);
        buf.writeBoolean(isQuery);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.setPacketHandled(true);

        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        // Move to Main loop
        ctx.enqueueWork(() -> {
            // 查询模式
            if (isQuery) {
                Level level = player.level;
                if (level.isLoaded(pos)) {
                    if (level.getBlockEntity(pos) instanceof ISmartBlockEntityControl control) {
                        control.createLazyTick$sendBlockUpdated();
                    }
                }
                return;
            }

            ClientData data = new ClientData(extraData, dimension, pos);

            // Packet Lock
            if (PacketCache.size() > 80) {
                mes.error("ServerPacket Cargo is full. This shouldn't happen!");
                PacketCache.clear();
            }

            // Remove the same
            for (ClientData existingData : PacketCache) {
                if (data.isSimilar(existingData))
                    return;
            }
            PacketCache.add(data);
        });
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
