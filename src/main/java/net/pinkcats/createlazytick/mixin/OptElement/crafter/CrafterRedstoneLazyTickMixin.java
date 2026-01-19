package net.pinkcats.createlazytick.mixin.OptElement.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickLogic;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import net.pinkcats.createlazytick.helper.ScheduleTicker;
import net.pinkcats.createlazytick.helper.extraDataTool.CrafterExtraDataTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;
import static net.pinkcats.createlazytick.helper.extraDataTool.CrafterExtraDataTool.packCrafterData;

@Mixin(value = MechanicalCrafterBlockEntity.class,remap = false)
public abstract class CrafterRedstoneLazyTickMixin extends SmartBlockEntity implements ISmartBlockEntityControl {

    @Shadow(remap = false)
    protected boolean wasPoweredBefore;

    @Unique
    private int lazytick$redstoneTick = 0;
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

    public CrafterRedstoneLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    private void lazytick$lowFrequencySync() {
        if (level == null) return;
        long time = level.getGameTime();
        boolean isPowered = this.lazytick$cachedSignal;

        boolean lazytick$cachedInWindow = lazytick$isInWindow(time, isPowered);

        boolean isDelayForced = this.createLazyTick$isDelayForced();
        this.lazytick$setExtraData(packCrafterData(isPowered, lazytick$cachedInWindow, isDelayForced));
    }

    @Unique
    private final ScheduleTicker LowFreq_Schedule = new ScheduleTicker(10, this::lazytick$lowFrequencySync);

    @Unique
    private boolean lazytick$isInWindow(long gameTime, boolean isPowered) {
        // 动态窗口检查
        // !wasPoweredBefore && isPowered -> 合成
        // 根据当前是否有被激活,决定活跃窗口期是2分钟还是10秒
        int currentWindow = isPowered ? WINDOW_POWERED : WINDOW_UNPOWERED;
        return (gameTime - lazytick$lastActiveTime < currentWindow);
    }

    @Unique
    private void lazytick$updateInterval(boolean signalChanged, boolean isPowered, long gameTime) {
        int maxDelay = Config.crafter_redstone_delay_max;

        // 信号发生改变,变回活跃状态 (刷新活跃时间)
        if (signalChanged) {
            lazytick$lastActiveTime = gameTime;
            LazyTickLogic.setIntervalSafe(this,1);
            return;
        }

        // 如果还在活跃期内,始终保持活跃检测
        if (lazytick$isInWindow(gameTime, isPowered)) {
            LazyTickLogic.setIntervalSafe(this,1);
            return;
        }

        // 不在窗口期时,累加延时检测
        int currentInterval = this.createLazyTick$getLazyTickInterval();
        if (currentInterval < maxDelay) {
            int newDelayTick = LazyTickLogic.computeNextInterval(this, currentInterval, maxDelay);

            if (newDelayTick != currentInterval) {
                LazyTickLogic.setIntervalSafe(this, newDelayTick);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void lazytick$onTickHead(CallbackInfo ci) {
        if (level == null || level.isClientSide) return;

        LowFreq_Schedule.RandomTick();

        NetworkSyncHelper.createLazyTick$syncPacketData(this,
                this.level, this.worldPosition, this.createLazyTick$getLazyTickInterval(), Config.crafter_redstone_delay_max);

        /*if (!level.isClientSide()) {
            System.out.println("Crafter:" + lazytick$redstoneTick + "/" + this.createLazyTick$getLazyTickInterval());
        }*/

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

        // 在懒加载期间时,则返回独立缓存的信号状态
        // 防止因 Mixin 跳过检测导致机器状态与真实信号不同步,进而引发每tick的极高频震荡(单独debug三秒给你刷几十上百KB)
        int interval = this.createLazyTick$getLazyTickInterval();
        if (interval > 1) {
            lazytick$redstoneTick++;
            if (lazytick$redstoneTick < interval) {
                return this.lazytick$cachedSignal;
            }
            lazytick$redstoneTick = 0;
        }

        long gameTime = level.getGameTime();

        // 重载时,按照正常逻辑返回
        if (IsServerReload) {
            lazytick$lastActiveTime = gameTime;
            LazyTickLogic.setIntervalSafe(this,1);
            lazytick$redstoneTick = 0;
            return level.hasNeighborSignal(pos);
        }

        // 执行正常返回逻辑并重置计时器(以及查看是否需要继续懒加载)
        boolean realSignal = level.hasNeighborSignal(pos);

        // 更新独立缓存
        this.lazytick$cachedSignal = realSignal;

        boolean changed = (realSignal != this.wasPoweredBefore);

        // 传入 realSignal 以决定使用长窗口还是短窗口
        lazytick$updateInterval(changed, realSignal, gameTime);

        return realSignal;
    }

    @Override
    public List<Component> createLazyTick$getCustomTooltipInfo() {
        List<Component> tooltip = new ArrayList<>();

        int data = this.lazytick$getExtraData();
        boolean isPowered = CrafterExtraDataTool.unpackIsPowered(data);
        boolean isInWindow = CrafterExtraDataTool.unpackInWindow(data);
        boolean isDelayForced = CrafterExtraDataTool.unpackIsDelayForced(data);

        if (isPowered) {
            tooltip.add(Component.literal("红石状态: 已激活").withStyle(ChatFormatting.RED));
            if (!isDelayForced) {
                if (isInWindow) {
                    tooltip.add(Component.literal("全速响应中(窗口期2分钟)").withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(Component.literal("闲置过久,已休眠").withStyle(ChatFormatting.RED));
                }
            } else {
                tooltip.add(Component.literal("已被强行控制懒加载时间上限,按照所选模式进行休眠").withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.literal(" 红石状态: 无信号").withStyle(ChatFormatting.DARK_GRAY));
            if (isInWindow) {
                if (isDelayForced) {
                    tooltip.add(Component.literal("已被强行控制懒加载时间上限,按照所选模式进行休眠").withStyle(ChatFormatting.GRAY));
                } else {
                    tooltip.add(Component.literal("全速响应中(窗口期10秒)").withStyle(ChatFormatting.GREEN));
                }
            }
        }

        tooltip.add(Component.literal("正常满槽位合成没有延迟,仅红石检测可能有延迟").withStyle(ChatFormatting.GRAY));
        return tooltip;
    }
}