package net.pinkcats.createlazytick.mixin.OptElement.saw;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawLazyTickMixin extends KineticBlockEntity implements ISmartBlockEntityControl {

    @Shadow(remap = false)
    public ProcessingInventory inventory;

    @Unique
    private int createLazyTick$sawTick = 0;

    @Unique
    private void createLazyTick$resetDelayTick() {
        createLazyTick$sawTick = 0;
        LazyTickLogic.setIntervalSafe(this, 1);
    }

    @Unique
    private void createLazyTick$applyBackoff() {
        createLazyTick$sawTick = 0;
        int currentInterval = this.createLazyTick$getCurrentSuperTick();
        int newInterval = LazyTickLogic.computeNextInterval(this, currentInterval, ServerConfig.getSawDelayMax());

        if (newInterval != currentInterval) {
            LazyTickLogic.setIntervalSafe(this, newInterval);
        }
    }

    public SawLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick_Head(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        NetworkSyncHelper.createLazyTick$syncPacketData(this,
                this.level, this.worldPosition, this.createLazyTick$getCurrentSuperTick(), ServerConfig.getSawDelayMax());

    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingKineticBlockEntity;tick()V",
            shift = At.Shift.AFTER), cancellable = true)
    public void createLazyTick$gateIdleTail(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        // 产出待重试、加工中、或有待启动输入时，Create 必须维持逐 tick 执行。
        if (!inventory.isEmpty() || inventory.remainingTime != -1) {
            createLazyTick$resetDelayTick();
            return;
        }

        createLazyTick$sawTick++;

        // 基类 tick 已完成；空库存的剩余 Saw 逻辑只会检查后返回，可安全延后。
        if (createLazyTick$sawTick < this.createLazyTick$getCurrentSuperTick()) {
            ci.cancel();
        } else {
            createLazyTick$applyBackoff();
        }
    }

}
