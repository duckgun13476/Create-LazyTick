package net.pinkcats.createlazytick.helper;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;

import javax.annotation.Nullable;

public class LazyTickLogic {
    @Nullable
    public static <T extends BlockEntityBehaviour> T getBehaviour(SmartBlockEntity be, Class<T> type) {
        if (be == null) return null;

        // 遍历查找，性能安全
        for (BlockEntityBehaviour behaviour : be.getAllBehaviours()) {
            if (type.isInstance(behaviour)) {
                return type.cast(behaviour);
            }
        }
        return null;
    }

    public static void switchMode(ISmartBlockEntityControl control, boolean isForcedMode, int value) {
        if (isForcedMode) {
            // === 切换到强制模式 ===
            // 1. 设置强制值 (0~100)
            control.createLazyTick$setForcedValue(value);
            // 2. [关键] 重置动态值为默认 (100)
            control.createLazyTick$setDynamicValue(100);
        } else {
            // === 切换到动态模式 ===
            // 1. 设置动态值
            control.createLazyTick$setDynamicValue(value);
            // 2. [关键] 关闭强制模式 (-1)
            control.createLazyTick$setForcedValue(-1);
        }
    }

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

        if (frc != -1) {
            ForcedActiveManager.register(level, pos);
            control.createLazyTick$setDelayForced(true);
            if (frc == 0) {
                // 强制全速 -> 间隔锁定为 1
                control.createLazyTick$setLazyTickInterval(1);
            } else {
                // 强制限制 -> 计算目标间隔
                int targetTick = Math.max(1, (int) ((frc / 100.0f) * maxConfigDelay));
                control.createLazyTick$setLazyTickInterval(targetTick);
            }
        } else { // 2. 动态模式
            control.createLazyTick$setDelayForced(false); // 标记为非强制

            if (dyn == 100) {
                // 默认模式 -> 移除监视
                ForcedActiveManager.unregister(level, pos);
                control.createLazyTick$setLazyTickInterval(1);
            }
            // ============================
            else {
                // 动态限制模式 (1% ~ 99%)
                ForcedActiveManager.register(level, pos);

                // 兜底：如果 dyn <= 0，通常应走 Forced 0，但这里设为 1 防止出问题
                if (dyn <= 0) {
                    control.createLazyTick$setLazyTickInterval(1);
                }
                // 正常的 Interval 计算交给 computeNextInterval
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
            //System.out.println("dont change");
            int frc = control.createLazyTick$getForcedValue();
            if (frc == 0) return 1; // [修改] 强制 0 返回 1
            return Math.max(1, (int) (maxConfigInterval * (frc / 100.0f)));
        }

        //System.out.println("change");
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