package net.pinkcats.createlazytick.mixin.OptElement.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Basin.IBasinOptimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasinBlockEntity.class,remap = false)
public abstract class BasinLazyTickMixin extends SmartBlockEntity implements IBasinOptimization {

    @Deprecated
    @Shadow
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
        if (lazytick$cachedHeatLevel == null) {
            if (level == null) return BlazeBurnerBlock.HeatLevel.NONE;
            BlockState stateBelow = level.getBlockState(worldPosition.below());
            lazytick$cachedHeatLevel = BlazeBurnerBlock.getHeatLevelOf(stateBelow);
        }
        return lazytick$cachedHeatLevel;
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void clt$onTick(CallbackInfo ci) {
        // 每 Tick 开始时重置缓存，确保数据实时性
        lazytick$cachedHeatLevel = null;
    }

    // Advanced version change can fix tick jump
    @Inject(method = "notifyChangeOfContents", at = @At("HEAD"), remap = false)
    private void clt$onNotifyChange(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyBasin()) return;
        optimization$inventoryVersion++;
    }
}