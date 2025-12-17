package net.pinkcats.createlazytick.mixin.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.helper.IBasinOptimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinBlockEntity.class)
public abstract class BasinLazyTickMixin extends SmartBlockEntity implements IBasinOptimization {

    @Shadow(remap = false)
    private boolean contentsChanged; // 引用原版的脏标记字段

    @Unique
    private long optimization$inventoryVersion = 0;

    // 缓存字段，模仿新版 Create (6.0.8有,编译也通过,但是游戏运行会炸,只能模仿)的行为 (Tick start -> null)
    @Unique
    private BlazeBurnerBlock.HeatLevel lazytick$cachedHeatLevel = null;

    // Mixin 继承 SmartBlockEntity 需要匹配构造函数
    public BasinLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public long getInventoryVersion() {
        return optimization$inventoryVersion;
    }

    //手动实现获取热量等级的方法
    //复刻了新版 BasinBlockEntity.getHeatLevel() 的逻辑
    //包含 NPE 检查和单 Tick 缓存机制
    @Override
    public BlazeBurnerBlock.HeatLevel optimization$getHeatLevel() {
        if (this.lazytick$cachedHeatLevel == null) {
            // 防范 NPE: 如果 level 为空，直接返回 NONE
            if (this.level == null) {
                return BlazeBurnerBlock.HeatLevel.NONE;
            }
            // 计算下方方块热量并写入缓存
            // distance: 1 -> below()
            BlockState stateBelow = this.level.getBlockState(this.worldPosition.below());
            this.lazytick$cachedHeatLevel = BlazeBurnerBlock.getHeatLevelOf(stateBelow);
        }
        return this.lazytick$cachedHeatLevel;
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void onTick(CallbackInfo ci) {
        // 每 Tick 开始时重置缓存，确保数据实时性
        this.lazytick$cachedHeatLevel = null;

        // 如果 Create 认为内容变了,让版本号 +1,否则维持原态(如果在没变化的情况下再次查询直接返回原状态值)
        if (!Config.enable_lazy_tick || !Config.enable_lazy_basin) return;
        if (this.contentsChanged) {
            optimization$inventoryVersion++;
        }
    }
}