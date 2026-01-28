package net.pinkcats.createlazytick.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class LazyTickSavedData extends SavedData {

    // 存档文件的名字
    private static final String DATA_FILE_NAME = "createlazytick_forced_machines";

    private final Set<BlockPos> forcedPositions = new HashSet<>();

    // 从NBT读取 (加载用)
    public static LazyTickSavedData load(CompoundTag nbt) {
        LazyTickSavedData data = new LazyTickSavedData();
        if (nbt.contains("ForcedPositions", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("ForcedPositions", Tag.TAG_LONG);
            for (Tag tag : list) {
                // 还原为 BlockPos
                data.forcedPositions.add(BlockPos.of(((LongTag) tag).getAsLong()));
            }
        }
        return data;
    }

    // 写入Nbt(保存用)
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        ListTag list = new ListTag();
        for (BlockPos pos : forcedPositions) {
            // long 格式的 blockPos
            list.add(LongTag.valueOf(pos.asLong()));
        }
        nbt.put("ForcedPositions", list);
        return nbt;
    }

    // Tool func
    public boolean add(BlockPos pos) {
        if (forcedPositions.add(pos)) {
            setDirty(); // 标记为"脏"，通知自动保存时写入磁盘
            return true;
        }
        return false;
    }

    public boolean remove(BlockPos pos) {
        if (forcedPositions.remove(pos)) {
            setDirty();
            return true;
        }
        return false;
    }

    public Set<BlockPos> getPositions() {
        return new HashSet<>(forcedPositions); // 返回副本防并发
    }

    //获取指定世界的存储实例
    public static LazyTickSavedData get(ServerLevel level) {
        // computeIfAbsent 自动处理加载或新建
        return level.getDataStorage().computeIfAbsent(
                LazyTickSavedData::load,
                LazyTickSavedData::new,
                DATA_FILE_NAME
        );
    }
}