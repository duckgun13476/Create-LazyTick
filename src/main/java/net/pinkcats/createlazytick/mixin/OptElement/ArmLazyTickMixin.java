package net.pinkcats.createlazytick.mixin.OptElement;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import net.pinkcats.createlazytick.helper.ScheduleTicker;
import net.pinkcats.createlazytick.helper.extradatatool.ArmExtraDataTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.pinkcats.createlazytick.CreateLazyTick.DropResourceLocation;
import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

/**
 * 机械臂 (Mechanical Arm) 的懒加载优化 Mixin
 * <p>
 * 优化策略：
 * 1. 状态监测：通过 Phase 检测机械臂是否处于"寻找输入/输出"的空闲状态。
 * 2. 深度缓存：将配置文件的 String ID 预解析为 Block 对象，运行时仅进行引用对比 (O(1))，避免昂贵的 Registry 查询和字符串操作。
 * 3. 错峰机制：初始化时引入随机计时器偏移 (Jitter)，防止大量机械臂在同一 Tick 同时执行扫描导致 TPS 骤降 (Thundering Herd Problem)。
 */
@Mixin(ArmBlockEntity.class)
public abstract class ArmLazyTickMixin extends SmartBlockEntity implements ISmartBlockEntityControl {

    @Shadow(remap = false)
    List<ArmInteractionPoint> inputs;
    @Shadow(remap = false)
    List<ArmInteractionPoint> outputs;
    @Shadow(remap = false)
    ArmBlockEntity.Phase phase;

    @Unique
    private int createLazyTick$armTick = 0;

    // 缓存状态：决定当前机械臂的运行模式
    @Unique
    private boolean createLazyTick$ignoreLazy = false; // true = 忽略懒加载，全速运行
    @Unique
    private boolean createLazyTick$weakLazy = false;   // true = 弱懒加载，使用较短的睡眠间隔

    // 重校验间隔: 10秒 = 200 ticks
    @Unique
    private static final int REVALIDATE_INTERVAL = 200;

    // 静态存储解析后的 Block 对象，所有机械臂实例共享。
    // 相比存储 String，这消除了运行时的 ForgeRegistries.getKey() 反查和 res.toString() 开销。
    @Unique private static Set<Block> createLazyTick$cachedIgnoreBlocks = null;
    @Unique private static Set<Block> createLazyTick$cachedWeakBlocks = null;

    public ArmLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 缓存构建方法
    // 将配置中的 String (如 "minecraft:chest") 解析为实际的 Block 对象并存入 Set。
    // 该操作较慢，但仅在首次运行或重载配置时执行一次。
    @Unique
    private void createLazyTick$rebuildCaches() {
        createLazyTick$cachedIgnoreBlocks = new HashSet<>();
        createLazyTick$cachedWeakBlocks = new HashSet<>();

        // 解析忽略列表
        for (String id : Config.arm_ignore_lazytick_list) {
            ResourceLocation loc = DropResourceLocation(id);
            if (ForgeRegistries.BLOCKS.containsKey(loc)) {
                createLazyTick$cachedIgnoreBlocks.add(ForgeRegistries.BLOCKS.getValue(loc));
            }
        }

        // 解析弱懒加载列表
        for (String id : Config.arm_weak_lazytick_list) {
            ResourceLocation loc = DropResourceLocation(id);
            if (ForgeRegistries.BLOCKS.containsKey(loc)) {
                createLazyTick$cachedWeakBlocks.add(ForgeRegistries.BLOCKS.getValue(loc));
            }
        }

    }

    // 维护缓存配置名单有效性
    @Unique
    private void createLazyTick$ensureConfigCaches() {
        // 情况1: 服务器重载配置 (IsServerReload)
        if (IsServerReload) {
            return;
        }

        // 情况2: 首次初始化
        if (createLazyTick$cachedIgnoreBlocks == null) {
            createLazyTick$rebuildCaches();
        }
    }


    // 判断方块是否在配置列表中
    // 直接对比 Block 对象的引用 (HashSet.contains),避免字符串类转换又查询的操作
    @Unique
    private boolean createLazyTick$isBlockInConfig(Block block, Set<Block> configSet) {
        if (block == null || configSet == null || configSet.isEmpty()) return false;
        return configSet.contains(block);
    }

    // 扫描逻辑
    // 遍历所有输入/输出点，判断是否接触了需要在"忽略懒加载列表"或"弱懒加载列表"中的容器。
    @Unique
    private void createLazyTick$scanUrgency() {
        if (level == null || level.isClientSide) return;

        createLazyTick$ensureConfigCaches();

        boolean foundIgnore = false;
        boolean foundWeak = false;

        // 检查输入端
        if (inputs != null) {
            for (ArmInteractionPoint point : inputs) {
                if (point == null) continue;
                // 获取世界中的方块状态 (必要开销,除非删了这玩意)
                BlockState state = level.getBlockState(point.getPos());
                Block block = state.getBlock();

                // 匹配
                if (createLazyTick$isBlockInConfig(block, createLazyTick$cachedIgnoreBlocks)) {
                    foundIgnore = true;
                    break; // 如果已经全速,无需再查弱懒加载的部分和未查的其他方块
                }
                if (createLazyTick$isBlockInConfig(block, createLazyTick$cachedWeakBlocks)) {
                    foundWeak = true;
                }
            }
        }

        // 检查输出端 (如果输入端已经决定全速，则跳过输出端检查)
        if (!foundIgnore && outputs != null) {
            for (ArmInteractionPoint point : outputs) {
                // 类似逻辑
                if (point == null) continue;
                BlockState state = level.getBlockState(point.getPos());
                Block block = state.getBlock();

                if (createLazyTick$isBlockInConfig(block, createLazyTick$cachedIgnoreBlocks)) {
                    foundIgnore = true;
                    break;
                }
                if (createLazyTick$isBlockInConfig(block, createLazyTick$cachedWeakBlocks)) {
                    foundWeak = true;
                }
            }
        }

        boolean oldIgnore = this.createLazyTick$ignoreLazy;
        boolean oldWeak = this.createLazyTick$weakLazy;

        this.createLazyTick$ignoreLazy = foundIgnore;
        this.createLazyTick$weakLazy = foundWeak;

        if ((foundIgnore && !oldIgnore) || (foundWeak && !oldWeak)) {
            this.createLazyTick$resetDelayTick();
        }
    }

    @Unique
    private void createLazyTick$UserControl() {
        NetworkSyncHelper.createLazyTick$processUserControl(this,Config.arm_delay_max);
    }

    @Unique
    private final ScheduleTicker UserControl_Schedule = new ScheduleTicker(5, this::createLazyTick$UserControl);

    @Unique
    private final ScheduleTicker ScanBlockType_Schedule = new ScheduleTicker(REVALIDATE_INTERVAL, this::createLazyTick$scanUrgency);

    @Unique
    private void createLazyTick$resetDelayTick() {
        createLazyTick$armTick = 0;

        if (this.createLazyTick$isDelayForced()) return;
        this.createLazyTick$setLazyTickInterval(1);
    }

    //初始化时执行一次扫描，并设置随机偏移
    @Inject(method = "initInteractionPoints", at = @At("RETURN"), remap = false)
    private void createLazyTick$onInitPoints(CallbackInfo ci) {
        createLazyTick$scanUrgency();
    }

    //定期检查环境 (每 10 秒)
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void createLazyTick$tickCheck(CallbackInfo ci) {
        if (level == null || level.isClientSide) return;

        UserControl_Schedule.RandomTick();

        NetworkSyncHelper.createLazyTick$syncPacketData(this,
                this.level, this.worldPosition, this.createLazyTick$getLazyTickInterval(), Config.arm_delay_max);

        ScanBlockType_Schedule.RandomTick();

        this.lazytick$setExtraData(ArmExtraDataTool.packArmData(createLazyTick$ignoreLazy, createLazyTick$weakLazy));
        /*if(!level.isClientSide()) {
            System.out.println("Arm:" + createLazyTick$armTick + "|" + this.createLazyTick$getLazyTickInterval());
        }*/
    }

    // 寻找输入懒加载计时器
    @Inject(method = "searchForItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$searchForItemHead(CallbackInfo ci) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_arm) return;

        if (createLazyTick$ignoreLazy) {
            this.createLazyTick$setLazyTickInterval(1);
            return;
        }

        createLazyTick$armTick++;
        if (createLazyTick$armTick < this.createLazyTick$getLazyTickInterval()) {
            ci.cancel();
        } else {
            createLazyTick$armTick = 0;
        }
    }

    // 寻找输入懒加载
    @Inject(method = "searchForItem", at = @At("RETURN"), remap = false)
    private void createLazyTick$searchForItemReturn(CallbackInfo ci) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_arm) return;
        if (createLazyTick$ignoreLazy) return;

        if (this.phase != ArmBlockEntity.Phase.SEARCH_INPUTS) {
            // 只要相位改变 (找到物品了)，立即重置等待，准备全速工作
            createLazyTick$resetDelayTick();
        } else {
            // 仍在寻找输入: 逐步增加睡眠时间
            int maxDelay = Config.arm_delay_max;
            if (createLazyTick$weakLazy) {
                maxDelay = Math.min(Config.arm_weak_delay_max, maxDelay);
            }

            int currentInterval = this.createLazyTick$getLazyTickInterval();

            if (currentInterval < maxDelay) {
                if (this.createLazyTick$isDelayForced()) return;
                int newInterval = Math.min(currentInterval + Math.max(1, currentInterval /10), maxDelay);
                this.createLazyTick$setLazyTickInterval(newInterval);
            }
        }
    }

    // 寻找输出懒加载计时器(注:机械臂搜索输入与搜索输出是互斥的)
    @Inject(method = "searchForDestination", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$searchForDestinationHead(CallbackInfo ci) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_arm) return;

        if (createLazyTick$ignoreLazy) {
            this.createLazyTick$setLazyTickInterval(1);
            return;
        }

        createLazyTick$armTick++;
        if (createLazyTick$armTick < this.createLazyTick$getLazyTickInterval()) {
            ci.cancel();
        } else {
            createLazyTick$armTick = 0;
        }
    }

    // 寻找输出懒加载逻辑
    @Inject(method = "searchForDestination", at = @At("RETURN"), remap = false)
    private void createLazyTick$searchForDestinationReturn(CallbackInfo ci) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_arm) return;
        if (createLazyTick$ignoreLazy) return;

        if (this.phase != ArmBlockEntity.Phase.SEARCH_OUTPUTS) {
            // 只要相位改变 (找到输出容器了)，立即重置
            createLazyTick$resetDelayTick();
        } else {
            // 仍在寻找输出: 增加睡眠时间
            int maxDelay = Config.arm_delay_max;
            if (createLazyTick$weakLazy) {
                maxDelay = Math.min(Config.arm_weak_delay_max, maxDelay);
            }

            int currentInterval = this.createLazyTick$getLazyTickInterval();

            if (currentInterval < maxDelay) {
                if (this.createLazyTick$isDelayForced()) return;
                int newInterval = Math.min(currentInterval + Math.max(1, currentInterval /10), maxDelay);
                this.createLazyTick$setLazyTickInterval(newInterval);
            }
        }
    }

    @Override
    public List<Component> createLazyTick$getCustomTooltipInfo() {
        List<Component> tooltip = new ArrayList<>();

        int data = this.lazytick$getExtraData();
        boolean ignore = ArmExtraDataTool.unpackIgnore(data);
        boolean weak = ArmExtraDataTool.unpackWeak(data);

        if (ignore) {
            tooltip.add(Component.literal("[配置豁免]全速响应").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("有目标方块在忽略懒加载名单内，已禁用懒加载").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("不可更改懒加载休眠间隔").withStyle(ChatFormatting.GOLD));
        } else if (weak) {
            tooltip.add(Component.literal("[配置豁免]弱懒加载").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("有目标方块在弱懒加载名单内，缩短休眠间隔").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("可通过强制指定状态更改懒加载休眠间隔").withStyle(ChatFormatting.GRAY));
        }

        return tooltip;
    }

    @Override
    public boolean createLazyTick$shouldRenderTier() {
        int data = this.lazytick$getExtraData();
        boolean ignore = ArmExtraDataTool.unpackIgnore(data);
        return !ignore;
    }

    @Override
    public boolean createLazyTick$shouldRenderMode() {
        int data = this.lazytick$getExtraData();
        boolean ignore = ArmExtraDataTool.unpackIgnore(data);
        return !ignore;
    }
}