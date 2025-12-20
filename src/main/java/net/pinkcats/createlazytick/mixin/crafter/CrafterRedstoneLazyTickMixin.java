package net.pinkcats.createlazytick.mixin.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@Mixin(value = MechanicalCrafterBlockEntity.class,remap = false)
public abstract class CrafterRedstoneLazyTickMixin {

    @Shadow(remap = false)
    protected boolean wasPoweredBefore;

    @Unique
    private int lazytick$redstoneTimer = 0;
    @Unique
    private int lazytick$currentInterval = 1;
    @Unique
    private long lazytick$lastActiveTime = -1;

    // 独立缓存:记住上次查到的真实信号,不依赖机器的 wasPoweredBefore,防止超高频震荡
    @Unique
    private boolean lazytick$cachedSignal = false;

    // 分级活跃窗口
    // 有信号:等待 2分钟(2400t) - 便于需要人工控制合成的情况(拉拉杆(?))
    @Unique
    private static final int WINDOW_POWERED = 2400;
    // 无信号:等待 10秒(200t) - 优化闲置机器
    @Unique
    private static final int WINDOW_UNPOWERED = 200;

    @Unique
    private void lazytick$updateInterval(boolean signalChanged, boolean isPowered, long gameTime) {
        int maxDelay = Config.crafter_redstone_delay_max;

        // 信号发生改变,变回活跃状态 (刷新活跃时间)
        if (signalChanged) {
            lazytick$lastActiveTime = gameTime;
            lazytick$currentInterval = 1;
            return;
        }

        // 动态窗口检查
        // !wasPoweredBefore && isPowered -> 合成
        // 根据当前是否有被激活,决定活跃窗口期是2分钟还是10秒
        int currentWindow = isPowered ? WINDOW_POWERED : WINDOW_UNPOWERED;

        // 如果还在活跃期内,始终保持活跃检测
        if (gameTime - lazytick$lastActiveTime < currentWindow) {
            lazytick$currentInterval = 1;
            return;
        }

        // 不在窗口期时,累加延时检测
        if (lazytick$currentInterval < maxDelay) {
            lazytick$currentInterval++;
        }
    }

    // 仅针对MechanicalCrafterBlockEntity中的hasNeighborSignal的执行方法
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"
            ),
            remap = true
    )
    private boolean lazytick$dynamicRedstoneCheck(Level level, BlockPos pos) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_crafter_redstone) {
            return level.hasNeighborSignal(pos);
        }

        long gameTime = level.getGameTime();

        // 重载时,按照正常逻辑返回
        if (IsServerReload) {
            lazytick$lastActiveTime = gameTime;
            lazytick$currentInterval = 1;
            lazytick$redstoneTimer = 0;
            return level.hasNeighborSignal(pos);
        }

        // 在懒加载期间时,若间隔仍大于0,则返回独立缓存的信号状态
        // 防止因 Mixin 跳过检测导致机器状态与真实信号不同步,进而引发每tick的极高频震荡(单独debug三秒给你刷几十上百KB)
        if (lazytick$redstoneTimer > 0) {
            lazytick$redstoneTimer--;
            return this.lazytick$cachedSignal;
        }

        // 执行正常返回逻辑并重置计时器(以及查看是否需要继续懒加载)
        boolean realSignal = level.hasNeighborSignal(pos);

        // 更新独立缓存
        this.lazytick$cachedSignal = realSignal;

        boolean changed = (realSignal != this.wasPoweredBefore);

        // 传入 realSignal 以决定使用长窗口还是短窗口
        lazytick$updateInterval(changed, realSignal, gameTime);

        // 设置计时器:Interval=1 时 Timer=0 (下tick继续查红石状态),Interval=2 时 Timer=1 (下tick跳过)
        lazytick$redstoneTimer = lazytick$currentInterval - 1;

        return realSignal;
    }
}