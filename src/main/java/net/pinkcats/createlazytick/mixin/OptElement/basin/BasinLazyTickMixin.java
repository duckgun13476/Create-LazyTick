package net.pinkcats.createlazytick.mixin.OptElement.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.pinkcats.createlazytick.bridge.Basin.IBasinOptimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BasinBlockEntity.class)
public abstract class BasinLazyTickMixin extends BlockEntity implements IBasinOptimization {

    @Shadow(remap = false) protected List<ItemStack> spoutputBuffer;
    @Shadow(remap = false) protected List<FluidStack> spoutputFluidBuffer;

    public BasinLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    private BlazeBurnerBlock.HeatLevel lazytick$cachedHeatLevel = null;

    //手动实现获取热量等级的方法
    //复刻了新版 BasinBlockEntity.getHeatLevel() 的逻辑
    //包含 NPE 检查和单 Tick 缓存机制
    @Override
    public BlazeBurnerBlock.HeatLevel clt$getHeatLevel() {
        if (lazytick$cachedHeatLevel == null) {
            if (level == null) return BlazeBurnerBlock.HeatLevel.NONE;
            BlockState stateBelow = level.getBlockState(worldPosition.below());
            lazytick$cachedHeatLevel = BlazeBurnerBlock.getHeatLevelOf(stateBelow);
        }
        return lazytick$cachedHeatLevel;
    }

    @Override
    public boolean clt$isOutputBufferEmpty() {
        return spoutputBuffer.isEmpty() && spoutputFluidBuffer.isEmpty();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void clt$onTick(CallbackInfo ci) {
        // 每 Tick 开始时重置缓存，确保数据实时性
        lazytick$cachedHeatLevel = null;
    }
}