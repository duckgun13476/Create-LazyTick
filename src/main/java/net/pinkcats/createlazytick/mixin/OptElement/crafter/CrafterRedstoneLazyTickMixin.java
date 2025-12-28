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
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import net.pinkcats.createlazytick.helper.ScheduleTicker;
import net.pinkcats.createlazytick.helper.extradatatool.CrafterExtraDataTool;
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
import static net.pinkcats.createlazytick.helper.extradatatool.CrafterExtraDataTool.packCrafterData;

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
    private void createLazyTick$UserControl() {
        NetworkSyncHelper.createLazyTick$processUserControl(this,Config.crafter_redstone_delay_max);
    }

    @Unique
    private final ScheduleTicker UserControl_Schedule = new ScheduleTicker(5, this::createLazyTick$UserControl);

    @Unique
    private void createLazyTick$safeChangeLazyTickInterval(int interval) {
        if (!this.createLazyTick$isDelayForced()) {
            this.createLazyTick$setLazyTickInterval(interval);
        }
    }

    @Unique
    private void lazytick$updateInterval(boolean signalChanged, boolean isPowered, long gameTime) {
        int maxDelay = Config.crafter_redstone_delay_max;

        // 动态窗口检查
        // !wasPoweredBefore && isPowered -> 合成
        // 根据当前是否有被激活,决定活跃窗口期是2分钟还是10秒
        int currentWindow = isPowered ? WINDOW_POWERED : WINDOW_UNPOWERED;
        boolean isInWindow = (gameTime - lazytick$lastActiveTime < currentWindow);
        boolean isDelayForced = this.createLazyTick$isDelayForced();
        this.lazytick$setExtraData(packCrafterData(isPowered, isInWindow, isDelayForced));

        // 信号发生改变,变回活跃状态 (刷新活跃时间)
        if (signalChanged) {
            lazytick$lastActiveTime = gameTime;
            createLazyTick$safeChangeLazyTickInterval(1);
            return;
        }

        // 如果还在活跃期内,始终保持活跃检测
        if (gameTime - lazytick$lastActiveTime < currentWindow) {
            createLazyTick$safeChangeLazyTickInterval(1);
            return;
        }

        // 不在窗口期时,累加延时检测
        int currentInterval = this.createLazyTick$getLazyTickInterval();
        if (currentInterval < maxDelay) {
            int newDelayTick = Math.min(currentInterval +
                    Math.max(1, currentInterval /10), Config.crafter_redstone_delay_max);
            createLazyTick$safeChangeLazyTickInterval(newDelayTick);
            currentInterval = newDelayTick;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void lazytick$onTickHead(CallbackInfo ci) {
        if (level == null || level.isClientSide) return;

        UserControl_Schedule.RandomTick();

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

        long gameTime = level.getGameTime();

        // 重载时,按照正常逻辑返回
        if (IsServerReload) {
            lazytick$lastActiveTime = gameTime;
            createLazyTick$safeChangeLazyTickInterval(1);
            lazytick$redstoneTick = 0;
            return level.hasNeighborSignal(pos);
        }

        // 在懒加载期间时,则返回独立缓存的信号状态
        // 防止因 Mixin 跳过检测导致机器状态与真实信号不同步,进而引发每tick的极高频震荡(单独debug三秒给你刷几十上百KB)
        if (lazytick$redstoneTick < this.createLazyTick$getLazyTickInterval()) {
            lazytick$redstoneTick++;
            return this.lazytick$cachedSignal;
        }

        // 执行正常返回逻辑并重置计时器(以及查看是否需要继续懒加载)
        boolean realSignal = level.hasNeighborSignal(pos);

        lazytick$redstoneTick = 0;

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