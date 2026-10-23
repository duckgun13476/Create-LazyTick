package net.pinkcats.createlazytick;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.pinkcats.createlazytick.Channel.LazyTickStatePacket;
import net.pinkcats.createlazytick.helper.ClockDisplayState;

/** Runs against each target's real Minecraft NBT/buffer and loader-specific response codec. */
public final class ClockDisplayStateRegression {
    public static void main(String[] args) throws Exception {
        int checks = 0;
        for (ClockDisplayState expected : new ClockDisplayState[]{
                new ClockDisplayState(1, 100, -1, 0, 0, ""),
                new ClockDisplayState(60, 50, 75, 31, 2, "Player"),
                new ClockDisplayState(Integer.MAX_VALUE, 100, 100, Integer.MAX_VALUE, 3, "测试玩家")}) {
            CompoundTag tag = expected.toTag();
            if (tag.getAllKeys().size() != 6 || tag.contains("Items") || tag.contains("cltUUID")) {
                throw new AssertionError("Snapshot includes non-display fields");
            }
            if (!expected.equals(ClockDisplayState.fromTag(tag))) throw new AssertionError("NBT round trip");
            tag.putString("Inventory", "must not be propagated");
            if (ClockDisplayState.fromTag(tag).toTag().contains("Inventory")) throw new AssertionError("NBT leak");
            checks += 3;

            var packet = LazyTickStatePacket.class.getConstructor(String.class, BlockPos.class, ClockDisplayState.class)
                    .newInstance("minecraft:overworld", new BlockPos(1, 64, 2), expected);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var encode = LazyTickStatePacket.class.getDeclaredMethod("encode", FriendlyByteBuf.class);
                encode.setAccessible(true);
                encode.invoke(packet, buf);
                if (buf.readableBytes() > 512) throw new AssertionError("Unexpectedly large display packet");
                var decoded = LazyTickStatePacket.class.getConstructor(FriendlyByteBuf.class).newInstance(buf);
                for (String name : new String[]{"dimension", "pos", "state"}) {
                    var field = LazyTickStatePacket.class.getDeclaredField(name);
                    field.setAccessible(true);
                    if (!field.get(packet).equals(field.get(decoded))) throw new AssertionError("Packet field " + name);
                    checks++;
                }
                if (buf.isReadable()) throw new AssertionError("Trailing packet data");
                checks += 2;
            } finally {
                buf.release();
            }
        }
        ClockDisplayState reset = ClockDisplayState.fromTag(new CompoundTag());
        if (reset.interval() != 1 || reset.dynamic() != 100 || reset.forced() != -1 || !reset.owner().isEmpty()
                || reset.extraData() != 0) throw new AssertionError("Missing-field defaults");
        System.out.println("Clock display state: " + (checks + 1) + " checks passed");
    }
}
