package net.pinkcats.createlazytick.manager;

import net.minecraft.nbt.CompoundTag;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class LazyTickStatCache {
    private final String blockName;     // 机器名称 ("创造马达")
    private final String ownerName;     // 调整玩家 ("Steve")
    private final long registeredTime;  // 调整时间 (timestamp)
    private final int scrollValue;      // 滚轮数值 (0~100)
    private final boolean isForced;     // 模式标记 (强制/动态)

    public LazyTickStatCache(String blockName, String ownerName, long registeredTime, int scrollValue, boolean isForced) {
        this.blockName = blockName;
        this.ownerName = ownerName;
        this.registeredTime = registeredTime;
        this.scrollValue = scrollValue;
        this.isForced = isForced;
    }

    public String getBlockName() { return blockName; }
    public String getOwnerName() { return ownerName; }
    public int getScrollValue() { return scrollValue; }
    public boolean isForced() { return isForced; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(registeredTime));
    }

    public long getRegisteredTime() {
        return registeredTime;
    }

    // 用于 SavedData 判断数据是否变化
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LazyTickStatCache that = (LazyTickStatCache) o;
        return registeredTime == that.registeredTime &&
                scrollValue == that.scrollValue &&
                isForced == that.isForced &&
                Objects.equals(blockName, that.blockName) &&
                Objects.equals(ownerName, that.ownerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockName, ownerName, registeredTime, scrollValue);
    }

    // 序列化存盘(nbt)
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", blockName);
        tag.putString("Owner", ownerName);
        tag.putLong("Time", registeredTime);
        tag.putInt("Scroll", scrollValue);
        tag.putBoolean("IsForced", isForced);
        return tag;
    }

    // 反序列化(从nbt(盘)读)
    public static LazyTickStatCache deserializeNBT(CompoundTag tag) {
        String name = tag.contains("Name") ? tag.getString("Name") : "未知机器";
        String owner = tag.contains("Owner") ? tag.getString("Owner") : "未知";
        long time = tag.contains("Time") ? tag.getLong("Time") : 0L;
        int scroll = tag.contains("Scroll") ? tag.getInt("Scroll") : 0;
        boolean forced = tag.contains("IsForced") && tag.getBoolean("IsForced");

        return new LazyTickStatCache(name, owner, time, scroll, forced);
    }
}