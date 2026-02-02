package net.pinkcats.createlazytick.manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class LazyTickSavedLimitList extends SavedData {

    private static final String DATA_FILE_NAME = "createlazytick_limits";

    // 存储玩家限制: 0 = 封禁, >0 = 限制个数, 不存在 = 无限制
    private final Map<String, Integer> playerLimits = new HashMap<>();

    public static LazyTickSavedLimitList load(CompoundTag nbt) {
        LazyTickSavedLimitList data = new LazyTickSavedLimitList();
        if (nbt.contains("PlayerLimits", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("PlayerLimits", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int limit = entry.getInt("Limit");
                if (limit >= 0) {
                    data.playerLimits.put(entry.getString("Name"), limit);
                }
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        ListTag list = new ListTag();
        playerLimits.forEach((name, limit) -> {
            if (limit >= 0) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Name", name);
                entry.putInt("Limit", limit);
                list.add(entry);
            }
        });
        nbt.put("PlayerLimits", list);
        return nbt;
    }

    public void setLimit(String name, int limit) {
        if (limit >= 0) {
            playerLimits.put(name, limit);
            setDirty();
        }
    }

    public void removeLimit(String name) {
        if (playerLimits.remove(name) != null) {
            setDirty();
        }
    }

    public int getLimit(String name) {
        return playerLimits.getOrDefault(name, -1);
    }

    @SuppressWarnings("resource")
    public static LazyTickSavedLimitList get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                LazyTickSavedLimitList::load,
                LazyTickSavedLimitList::new,
                DATA_FILE_NAME
        );
    }
}