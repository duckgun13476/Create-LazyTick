package net.pinkcats.createlazytick.manager;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;

import java.util.Collections;
import java.util.List;
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
            LazyTickSavedStat savedData = LazyTickSavedStat.get(serverLevel);

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
            if(LazyTickSavedStat.get(serverLevel).add(pos, info)) {
                dataVersion.incrementAndGet(); // 更新版本号(缓存)
            }
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if(LazyTickSavedStat.get(serverLevel).remove(pos)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    public static int executeBatchReset(ServerLevel level, List<BlockPos> targets) {
        if (targets.isEmpty()) return 0;

        int validResetCount = 0;
        int dirtyDataCount = 0;
        boolean dataChanged = false; // 标记是否改动了存档(包含清理脏数据)
        LazyTickSavedStat data = LazyTickSavedStat.get(level);

        for (BlockPos pos : targets) {
            // 确保区块已加载(避免异步过程中区块被卸载导致状态过时/非法)
            if (level.isLoaded(pos)) {

                // 1. 尝试获取并重置机器 (如果存在)
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ISmartBlockEntityControl control && !control.lazytick$isDefaultState()) {
                    dataChanged = true;
                    // 工具方法,内部处理模式转换
                    LazyTickLogic.switchMode(control, false, 100);
                    validResetCount++;
                    // 重置UI
                    if (control instanceof SmartBlockEntity sbe) {
                        LazyTickScrollBehaviour behaviour = LazyTickLogic.getBehaviour(sbe, LazyTickScrollBehaviour.class);
                        if (behaviour != null) {
                            behaviour.setValue(100);
                        }
                    }
                }

                // 2. 移除脏数据记录
                if (data.remove(pos)) {
                    dataChanged = true;
                    dirtyDataCount++;
                }

            }
        }

        if (dataChanged) {
            dataVersion.incrementAndGet();
        }
        CreateLazyTick.LOGGER.info("Cleared {} invalid lazytick data entries", dirtyDataCount);

        return validResetCount;
    }

    //return : from Set<BlockPos> to Map<BlockPos, LazyTickStatCache>(可以获取位置和具体信息)
    public static Map<BlockPos, LazyTickStatCache> getForcedMachines(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return LazyTickSavedStat.get(serverLevel).getMachinesMap();
        }
        return Collections.emptyMap();
    }

    public static long getVersion() {
        return dataVersion.get();
    }
}