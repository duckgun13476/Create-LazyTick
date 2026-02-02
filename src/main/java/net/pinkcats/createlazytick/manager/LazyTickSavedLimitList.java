package net.pinkcats.createlazytick.manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LazyTickSavedLimitList extends SavedData {

    private static final String DATA_FILE_NAME = "createlazytick_limits";

    // 存储玩家限制: 0 = 封禁, >0 = 限制个数, 不存在 = 无限制
    private final Map<UUID, Integer> playerLimits = new HashMap<>();

    public static LazyTickSavedLimitList load(CompoundTag nbt) {
        LazyTickSavedLimitList data = new LazyTickSavedLimitList();
        if (nbt.contains("PlayerLimits", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("PlayerLimits", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int limit = entry.getInt("Limit");
                if (limit >= 0 && entry.hasUUID("UUID")) {
                    data.playerLimits.put(entry.getUUID("UUID"), limit);
                }
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        ListTag list = new ListTag();
        playerLimits.forEach((uuid, limit) -> {
            if (limit >= 0) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                entry.putInt("Limit", limit);
                list.add(entry);
            }
        });
        nbt.put("PlayerLimits", list);
        return nbt;
    }

    public void setLimit(UUID uuid, int limit) {
        if (limit >= 0) {
            playerLimits.put(uuid, limit);
            setDirty();
        }
    }

    public void removeLimit(UUID uuid) {
        if (playerLimits.remove(uuid) != null) {
            setDirty();
        }
    }

    public int getLimit(UUID uuid) {
        return playerLimits.getOrDefault(uuid, -1);
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