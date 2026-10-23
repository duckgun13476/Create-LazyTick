package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.pinkcats.createlazytick.helper.ClockDisplayState;

public record LazyTickStatePacket(String dimension, BlockPos pos, ClockDisplayState state) {
    public LazyTickStatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readBlockPos(), ClockDisplayState.fromTag(buf.readNbt()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeNbt(state.toTag());
    }
}
