package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ClockSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClockSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("createlazytick", "clock_sync"));

    public static final StreamCodec<FriendlyByteBuf, ClockSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ClockSyncPacket::encode, ClockSyncPacket::new);

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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {

        Player player = ctx.player();//<-
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Move to Main loop
        ctx.enqueueWork(() -> {
            // 查询模式
            if (isQuery) {
                Level level = serverPlayer.level();
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
