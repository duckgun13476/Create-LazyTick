package net.pinkcats.createlazytick.helper;

import net.minecraft.nbt.CompoundTag;

/** Display-only snapshot. Never contains inventory data or calls a block entity's NBT reader. */
public record ClockDisplayState(int interval, int dynamic, int forced, int extraData, int tier, String owner) {
    public ClockDisplayState {
        interval = Math.max(1, interval);
        owner = owner == null ? "" : owner;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("cltCurrentInterval", interval);
        tag.putInt("cltDynamic", dynamic);
        tag.putInt("cltForced", forced);
        tag.putInt("cltExtraData", extraData);
        tag.putInt("cltTier", tier);
        tag.putString("cltOwner", owner);
        return tag;
    }

    public static ClockDisplayState fromTag(CompoundTag tag) {
        if (tag == null) throw new IllegalArgumentException("Missing clock display state");
        return new ClockDisplayState(tag.getInt("cltCurrentInterval"),
                tag.contains("cltDynamic") ? tag.getInt("cltDynamic") : 100,
                tag.contains("cltForced") ? tag.getInt("cltForced") : -1,
                tag.getInt("cltExtraData"), tag.getInt("cltTier"), tag.getString("cltOwner"));
    }
}
