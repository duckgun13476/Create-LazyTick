package net.pinkcats.createlazytick.manager;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.pinkcats.createlazytick.Gui.mes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class LazyTickStatCache {
    private final String blockId;     // 机器名称(id) ("create:mechanical_saw")
    private final UUID ownerUUID;       // 调整玩家的uuid(鉴权)
    private final String ownerName;     // 调整玩家 ("Steve")
    private final long registeredTime;  // 调整时间 (timestamp)
    private final int scrollValue;      // 滚轮数值 (0~100)
    private final boolean isForced;     // 模式标记 (强制/动态)

    public LazyTickStatCache(String blockId, UUID ownerUUID, String ownerName, long registeredTime,
                             int scrollValue, boolean isForced) {
        this.blockId = blockId;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.registeredTime = registeredTime;
        this.scrollValue = scrollValue;
        this.isForced = isForced;
    }

    public String getBlockId() { return blockId; }
    public UUID getOwnerUUID() { return ownerUUID; }
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
            ResourceLocation rl = ResourceLocation.tryParse(this.blockId);
            if (rl != null) {
                // 从注册表中查找方块
                Block block = BuiltInRegistries.BLOCK.get(rl);
                // 如果方块存在且不是空气
                if (block != Blocks.AIR) {
                    // 返回名称(可翻译组件)
                    return block.getName();
                }
            }
        } catch (Exception e) {
            mes.error("Error occurred when trying to parse the block id: "+ e.getMessage());
        }
        // 回退,抛出注册id
        return mes.Char(this.blockId);
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
                Objects.equals(blockId, that.blockId) &&
                Objects.equals(ownerUUID, that.ownerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockId, ownerUUID, registeredTime, scrollValue, isForced);
    }

    // 序列化存盘(nbt)
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", blockId);
        tag.putUUID("OwnerUUID", ownerUUID);
        tag.putString("Owner", ownerName);
        tag.putLong("Time", registeredTime);
        tag.putInt("Scroll", scrollValue);
        tag.putBoolean("IsForced", isForced);
        return tag;
    }

    // 反序列化(从nbt(盘)读)
    public static LazyTickStatCache deserializeNBT(CompoundTag tag) {
        String name = tag.contains("Name") ? tag.getString("Name") : "Unknown";
        UUID ownerUUID = tag.contains("OwnerUUID") ? tag.getUUID("OwnerUUID") : Util.NIL_UUID;
        String owner = tag.contains("Owner") ? tag.getString("Owner") : "Unknown";
        long time = tag.contains("Time") ? tag.getLong("Time") : 0L;
        int scroll = tag.contains("Scroll") ? tag.getInt("Scroll") : 0;
        boolean forced = tag.contains("IsForced") && tag.getBoolean("IsForced");

        return new LazyTickStatCache(name, ownerUUID, owner, time, scroll, forced);
    }
}