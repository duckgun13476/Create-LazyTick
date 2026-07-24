package net.pinkcats.createlazytick.mixin.OptElement.saw;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.adaptive.SawAdaptiveSchedule;
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
    private boolean createLazyTick$inventoryChanged = false;

    @Unique
    private boolean createLazyTick$outputAttempted = false;

    @Unique
    private SawAdaptiveSchedule.State createLazyTick$adaptiveSchedule = SawAdaptiveSchedule.State.initial();

    @Unique
    private void createLazyTick$resetDelayTick() {
        createLazyTick$sawTick = 0;
        LazyTickLogic.setIntervalSafe(this, 1);
    }

    @Unique
    private void createLazyTick$applyBackoff() {
        createLazyTick$sawTick = 0;
        int maxInterval = ServerConfig.getSawDelayMax();
        createLazyTick$adaptiveSchedule = SawAdaptiveSchedule.onRetryFailure(createLazyTick$adaptiveSchedule, maxInterval);
        int nextInterval = SawAdaptiveSchedule.nextProbeInterval(
                createLazyTick$adaptiveSchedule, level.getGameTime(), maxInterval, 2);
        LazyTickLogic.setIntervalSafe(this, nextInterval);
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

        createLazyTick$inventoryChanged = false;
        createLazyTick$outputAttempted = false;
        if (inventory.isEmpty()) {
            int emptyResetTicks = (int) Math.min(Integer.MAX_VALUE, (long) ServerConfig.getSawDelayMax() * 2L);
            createLazyTick$adaptiveSchedule = SawAdaptiveSchedule.expireAfterEmptyIdle(
                    createLazyTick$adaptiveSchedule, level.getGameTime(), emptyResetTicks);
        }
    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/SawBlockEntity;getItemMovementVec()Lnet/minecraft/world/phys/Vec3;"), cancellable = true)
    public void createLazyTick$gateBlockedOutputRetry(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        // 到达此处代表 Create 已完成加工倒计时，正准备输出或抛出产物。
        // 尚未阻塞时第一次必须放行；失败后由 onOutputFail 逐步增加下一次重试间隔。
        if (inventory.remainingTime > 0 || inventory.isEmpty()) {
            createLazyTick$resetDelayTick();
            return;
        }

        createLazyTick$sawTick++;

        // 已确认处于输出重试状态时，使用受配置上限约束的退避轮询。
        if (createLazyTick$sawTick < this.createLazyTick$getCurrentSuperTick()) {
            ci.cancel();
        } else {
            // 放行一次实际重试；只有 RETURN 仍未产生库存变化时才增长退避。
            createLazyTick$sawTick = 0;
            createLazyTick$outputAttempted = true;
        }
    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/recipe/ProcessingInventory;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"))
    public void createLazyTick$onInventoryChange(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        createLazyTick$inventoryChanged = true;
        if (createLazyTick$outputAttempted) {
            createLazyTick$adaptiveSchedule = SawAdaptiveSchedule.onOutputSuccess(
                    createLazyTick$adaptiveSchedule, level.getGameTime(), ServerConfig.getSawDelayMax());
        }
        createLazyTick$resetDelayTick();
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void createLazyTick$onOutputFail(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        if (createLazyTick$outputAttempted && inventory.remainingTime == 0 && !inventory.isEmpty()
                && !createLazyTick$inventoryChanged)
            createLazyTick$applyBackoff();
    }

}
