package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pinkcats.createlazytick.helper.ClockDisplayState;
import org.jetbrains.annotations.NotNull;

public class LazyTickStatePacket implements CustomPacketPayload {
    public static final Type<LazyTickStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("createlazytick", "lazytick_state"));

    public static final StreamCodec<FriendlyByteBuf, LazyTickStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(LazyTickStatePacket::encode, LazyTickStatePacket::new);

    private final String dimension;
    private final BlockPos pos;
    private final ClockDisplayState state;

    public LazyTickStatePacket(String dimension, BlockPos pos, ClockDisplayState state) {
        this.dimension = dimension;
        this.pos = pos;
        this.state = state;
    }

    public LazyTickStatePacket(FriendlyByteBuf buf) {
        this.dimension = buf.readUtf();
        this.pos = buf.readBlockPos();
        CompoundTag readState = buf.readNbt();
        this.state = ClockDisplayState.fromTag(readState);
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeNbt(state.toTag());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientClockStateHandler.accept(dimension, pos, state));
    }
}
