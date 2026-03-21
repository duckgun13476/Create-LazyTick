package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;
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
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(blockEntity);
                        CompoundTag state = blockEntity.saveWithoutMetadata(level.registryAccess());
                        ISmartBlockEntityControl control = blockEntity instanceof ISmartBlockEntityControl smart
                                ? smart
                                : SmartLazyTickStateHelper.control(blockEntity);
                        if (control != null) {
                            state.putInt("cltCurrentInterval", Math.max(1, control.createLazyTick$getCurrentSuperTick()));
                            state.putInt("cltDynamic", control.createLazyTick$getDynamicValue());
                            state.putInt("cltForced", control.createLazyTick$getForcedValue());
                            state.putInt("cltExtraData", control.lazytick$getExtraData());
                            state.putInt("cltTier", control.lazytick$getSyncedTier().ordinal());
                            String owner = control.createLazyTick$getOwnerName();
                            if (owner != null && !owner.isEmpty()) {
                                state.putString("cltOwner", owner);
                            }
                            control.createLazyTick$sendBlockUpdated();
                        } else if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                            mes.warn("[Network][clock_sync][server] depot query target is not ISmartBlockEntityControl: "
                                    + blockEntity.getClass().getName());
                        }
                        if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                            mes.info("[Network][clock_sync][server] sending state for depot keys=" + state.getAllKeys());
                        }
                        CLTChannel.sendToPlayer(
                                new LazyTickStatePacket(level.dimension().location().toString(), pos, state),
                                serverPlayer
                        );
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
