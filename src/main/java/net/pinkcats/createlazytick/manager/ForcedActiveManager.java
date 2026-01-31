package net.pinkcats.createlazytick.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : *Fugit-5414*
 * <p>
 * Global class for save manipulated lazytick's machine
 * Thread secure
 */
public class ForcedActiveManager {

    private static final AtomicLong dataVersion = new AtomicLong(0);

    public static void register(Level level, BlockPos pos, String blockName, String ownerName, int scrollValue, boolean isForced) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            // 1. 获取存档管理器
            LazyTickSavedData savedData = LazyTickSavedData.get(serverLevel);

            // 2. 检查该位置是否已经有记录
            LazyTickStatCache existingInfo = savedData.getMachinesMap().get(pos);

            long timeToRecord = System.currentTimeMillis();

            // 3. 判断已有信息的时间是否需要更新
            if (existingInfo != null) {
                // 如果拥有者,数值,模式都没有变化,则不更新
                boolean isSameOwner = existingInfo.getOwnerName().equals(ownerName);
                boolean isSameValue = existingInfo.getScrollValue() == scrollValue;
                boolean isSameMode = existingInfo.isForced() == isForced;

                if (isSameOwner && isSameValue && isSameMode) {
                    // 复用旧时间戳
                    timeToRecord = existingInfo.getRegisteredTime();
                }
            }

            // 1. 构建详细信息缓存Obj
            LazyTickStatCache info = new LazyTickStatCache(
                    blockName,
                    ownerName,
                    timeToRecord, // 记录真正更改时的时间戟(由重启导致的重新注册不算在内)
                    scrollValue,
                    isForced
            );

            // 2. 存入 SavedData
            // 如果数据发生变化,add 会返回 true
            if(LazyTickSavedData.get(serverLevel).add(pos, info)) {
                dataVersion.incrementAndGet(); // 更新版本号(缓存)
            }
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if(LazyTickSavedData.get(serverLevel).remove(pos)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    //return : from Set<BlockPos> to Map<BlockPos, LazyTickStatCache>(可以获取位置和具体信息)
    public static Map<BlockPos, LazyTickStatCache> getForcedMachines(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return LazyTickSavedData.get(serverLevel).getMachinesMap();
        }
        return Collections.emptyMap();
    }

    public static long getVersion() {
        return dataVersion.get();
    }
}