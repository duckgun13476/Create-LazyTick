package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.createlazytick.helper.ClockDisplayState;
import java.util.function.Supplier;

public record LazyTickStatePacket(String dimension, BlockPos pos, ClockDisplayState state) {
    public LazyTickStatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readBlockPos(), ClockDisplayState.fromTag(buf.readNbt()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeNbt(state.toTag());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.setPacketHandled(true);
        if (ctx.getDirection() != NetworkDirection.PLAY_TO_CLIENT) return;
        ctx.enqueueWork(() -> ClientClockStateHandler.accept(dimension, pos, state));
    }
}
