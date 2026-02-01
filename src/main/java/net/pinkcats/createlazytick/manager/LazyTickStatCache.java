package net.pinkcats.createlazytick.manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.CreateLazyTick;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class LazyTickStatCache {
    private final String blockName;     // 机器名称(id) (create:mechanical_saw")
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

    public Component getDisplayName() {
        try {
            // 尝试将存储的 blockName(id) 转为资源路径(e.g. "create:creative_motor")
            ResourceLocation rl = ResourceLocation.tryParse(this.blockName);
            if (rl != null) {
                // 从注册表中查找方块
                Block block = ForgeRegistries.BLOCKS.getValue(rl);
                // 如果方块存在且不是空气
                if (block != null && block != Blocks.AIR) {
                    // 返回名称(可翻译组件)
                    return block.getName();
                }
            }
        } catch (Exception e) {
            CreateLazyTick.LOGGER.error("Error occurred when trying to parse the block id:\n{}",e.getMessage());
        }
        // 回退,抛出注册id
        return Component.literal(this.blockName);
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
        String name = tag.contains("Name") ? tag.getString("Name") : "未知元件";
        String owner = tag.contains("Owner") ? tag.getString("Owner") : "未知";
        long time = tag.contains("Time") ? tag.getLong("Time") : 0L;
        int scroll = tag.contains("Scroll") ? tag.getInt("Scroll") : 0;
        boolean forced = tag.contains("IsForced") && tag.getBoolean("IsForced");

        return new LazyTickStatCache(name, owner, time, scroll, forced);
    }
}