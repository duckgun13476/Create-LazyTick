package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pinkcats.createlazytick.Gui.mes;
import org.jetbrains.annotations.NotNull;

public class LazyTickStatePacket implements CustomPacketPayload {
    public static final Type<LazyTickStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("createlazytick", "lazytick_state"));

    public static final StreamCodec<FriendlyByteBuf, LazyTickStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(LazyTickStatePacket::encode, LazyTickStatePacket::new);

    private final String dimension;
    private final BlockPos pos;
    private final CompoundTag state;

    public LazyTickStatePacket(String dimension, BlockPos pos, CompoundTag state) {
        this.dimension = dimension;
        this.pos = pos;
        this.state = state == null ? new CompoundTag() : state.copy();
    }

    public LazyTickStatePacket(FriendlyByteBuf buf) {
        this.dimension = buf.readUtf();
        this.pos = buf.readBlockPos();
        CompoundTag readState = buf.readNbt();
        this.state = readState == null ? new CompoundTag() : readState;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeNbt(state);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LazyTickClientStateCache.put(dimension, pos, state);
            if (state.contains("cltCurrentInterval")
                    || state.contains("cltDynamic")
                    || state.contains("cltForced")
                    || state.contains("cltExtraData")) {
                mes.debug("[Network][lazytick_state][client] received state pos=" + pos + " keys=" + state.getAllKeys());
            }
        });
    }
}
