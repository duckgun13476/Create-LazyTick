package net.pinkcats.createlazytick.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;

public class LazyTickLogic {
    // Entry Point: Called on Init or Player Interaction to register state & apply limits.
    // 入口点:在初始化或玩家交互时调用,用于注册状态并应用配置限制
    /**
     * Use this when block's lazytick status is changed.
     * @param control 接口实例
     */
    public static void updateState(ISmartBlockEntityControl control) {
        if (!(control instanceof BlockEntity be) || be.getLevel() == null || be.getLevel().isClientSide) {
            return;
        }

        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(be);
        if (whiteItem == null) {
            return;
        }
        int maxConfigDelay = whiteItem.getMaxTick(); // get config max delay

        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();

        int dyn = control.createLazyTick$getDynamicValue();
        int frc = control.createLazyTick$getForcedValue();

        // 2. Logic A: 是否需要列入“监测名单”
        // default status: Dynamic = 100, Forced = -1
        boolean isDefault = (dyn == 100 && frc == -1);

        if (isDefault) {
            // default -> remove from list
            ForcedActiveManager.unregister(level, pos);

            control.createLazyTick$setDelayForced(false);
            control.createLazyTick$setLazyTickInterval(1);
        } else {
            // non-default -> add to list
            ForcedActiveManager.register(level, pos);

            // 3. Logic B: compute delay
            if (frc > 0) {
                // Forced delay mode
                // (percentage / 100.0) * max_delay from config
                int targetTick = (int) ((frc / 100.0f) * maxConfigDelay);
                targetTick = Math.max(1, targetTick);

                control.createLazyTick$setLazyTickInterval(targetTick);
                control.createLazyTick$setDelayForced(true);

            } else if (dyn > 0) {
                // Dynamic delay mode
                // 动态模式的具体计算在 computeNextInterval & setIntervalSafe中
                control.createLazyTick$setDelayForced(false);
            } else {
                // 0 -> disable
                control.createLazyTick$setLazyTickInterval(1);
                control.createLazyTick$setDelayForced(true);
            }
        }
    }

    // Runtime Logic: Computes the dynamic interval for the next tick cycle.
    // 运行时逻辑:计算下一个 Tick 循环的动态间隔时间
    /**
     * Computes the next backoff interval.</p>
     * Integrates: Forced mode check, exponential backoff (+10%), and dynamic limit restrictions.<br><i>
     * 强制模式检查、指数退避(+10%)、动态上限限制
     */
    public static int computeNextInterval(ISmartBlockEntityControl control, int currentInterval, int maxConfigInterval) {
        // 1. if mode is Forced -> Interval won't change(return immediately)
        if (control.createLazyTick$isDelayForced()) {
            return currentInterval;
        }

        // 2. compute next interval (指数退避: 10%)
        int nextInterval = currentInterval + Math.max(1, currentInterval / 10);

        // 3. compute dynamic max value limit
        int dynamicPercent = control.createLazyTick$getDynamicValue();
        // normally, it prevents status error(if block is forced,it will return in step 1)
        if (dynamicPercent <= 0) dynamicPercent = 100;

        int effectiveMax = (int) (maxConfigInterval * (dynamicPercent / 100.0f));
        effectiveMax = Math.max(1, effectiveMax);

        // 4. return min of(next interval from compute & max limit)
        return Math.min(nextInterval, effectiveMax);
    }

    /**
     * When a block entity is not forced,its interval can be set.
     */
    public static void setIntervalSafe(ISmartBlockEntityControl control, int interval) {
        if (!control.createLazyTick$isDelayForced()) {
            control.createLazyTick$setLazyTickInterval(interval);
        }
    }
}