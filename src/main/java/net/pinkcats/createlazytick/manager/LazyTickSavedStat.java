package net.pinkcats.createlazytick.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LazyTickSavedStat extends SavedData {

    // 存档文件的名字
    private static final String DATA_FILE_NAME = "createlazytick_forced_machines";

    // 从纯坐标升级为<坐标,详细信息缓存>
    private final Map<BlockPos, LazyTickStatCache> forcedMachines = new ConcurrentHashMap<>();

    // 从NBT读取 (加载用)
    public static LazyTickSavedStat load(CompoundTag nbt) {
        LazyTickSavedStat data = new LazyTickSavedStat();
        if (nbt.contains("Machines", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("Machines", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));

                // 读取详细信息(写入Map)
                LazyTickStatCache info = LazyTickStatCache.deserializeNBT(entry.getCompound("Info"));
                data.forcedMachines.put(pos, info);
            }
        }
        return data;
    }

    // 写入Nbt(保存用)
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        ListTag list = new ListTag();
        forcedMachines.forEach((pos, info) -> {
            CompoundTag entry = new CompoundTag();
            // 存坐标
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            // 存详细信息
            entry.put("Info", info.serializeNBT());

            list.add(entry);
        });

        nbt.put("Machines", list);
        return nbt;
    }

    // Tool func
    public boolean add(BlockPos pos, LazyTickStatCache info) {
        if (!forcedMachines.containsKey(pos) || !forcedMachines.get(pos).equals(info)) {
            forcedMachines.put(pos, info);
            setDirty(); // 标记需要存盘

            return true;
        }
        return false;
    }

    public boolean remove(BlockPos pos) {
        if (forcedMachines.remove(pos) != null) {
            setDirty();

            return true;
        }
        return false;
    }

    // 获取完整映射表
    public Map<BlockPos, LazyTickStatCache> getMachinesMap() {
        return new HashMap<>(forcedMachines);  // 返回副本防并发
    }

    //获取指定世界的存储实例
    public static LazyTickSavedStat get(ServerLevel level) {
        // computeIfAbsent 自动处理加载或新建
        return level.getDataStorage().computeIfAbsent(
                LazyTickSavedStat::load,
                LazyTickSavedStat::new,
                DATA_FILE_NAME
        );
    }
}