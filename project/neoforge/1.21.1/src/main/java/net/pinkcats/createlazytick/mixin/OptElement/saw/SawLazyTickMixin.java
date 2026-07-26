package net.pinkcats.createlazytick.mixin.OptElement.saw;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.adaptive.saw.SawFrequencyFunction;
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

    @Shadow(remap = false)
    protected abstract boolean canProcess();

    @Unique
    private int createLazyTick$sawTick = 0;

    @Unique
    private boolean createLazyTick$inventoryChanged = false;

    @Unique
    private boolean createLazyTick$outputAttempted = false;

    @Unique
    private boolean createLazyTick$allowNextOutputAttempt = true;

    @Unique
    private SawFrequencyFunction.State createLazyTick$adaptiveSchedule = SawFrequencyFunction.State.initial();

    @Unique
    private int createLazyTick$idleInputTick = 0;

    @Unique
    private SawFrequencyFunction.State createLazyTick$inputSchedule = SawFrequencyFunction.State.initial();

    @Unique
    private void createLazyTick$resetDelayTick() {
        createLazyTick$sawTick = 0;
        LazyTickLogic.setIntervalSafe(this, 1);
    }

    @Unique
    private void createLazyTick$applyBackoff() {
        createLazyTick$sawTick = 0;
        int maxInterval = ServerConfig.getSawDelayMax();
        createLazyTick$adaptiveSchedule = SawFrequencyFunction.onRetryFailure(createLazyTick$adaptiveSchedule, maxInterval);
        int nextInterval = SawFrequencyFunction.nextProbeInterval(
                createLazyTick$adaptiveSchedule, level.getGameTime(), maxInterval, 2);
        LazyTickLogic.setIntervalSafe(this, nextInterval);
    }

    @Unique
    private void createLazyTick$wakeForInput() {
        createLazyTick$idleInputTick = 0;
        int currentInterval = this.createLazyTick$getCurrentSuperTick();
        createLazyTick$inputSchedule = SawFrequencyFunction.onInputArrival(
                createLazyTick$inputSchedule, level.getGameTime(), ServerConfig.getSawDelayMax(), currentInterval);
        createLazyTick$sawTick = 0;
        LazyTickLogic.setIntervalSafe(this, SawFrequencyFunction.reduceAfterInput(currentInterval));
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
            createLazyTick$adaptiveSchedule = SawFrequencyFunction.expireAfterEmptyIdle(
                    createLazyTick$adaptiveSchedule, level.getGameTime(), emptyResetTicks);
        }
    }

    /**
     * The saw's own processing countdown must stay per-tick, but a top-facing saw with no input
     * has no work to perform after its base tick.  {@code start(ItemStack)} below is the wake-up
     * edge for belt, capability, and entity input.
     */
    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/SawBlockEntity;canProcess()Z"), cancellable = true)
    public void createLazyTick$gateEmptyInputIdle(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide || !canProcess()) return;

        if (!inventory.isEmpty()) {
            createLazyTick$idleInputTick = 0;
            return;
        }

        createLazyTick$idleInputTick++;
        if (createLazyTick$idleInputTick < this.createLazyTick$getCurrentSuperTick()) {
            ci.cancel();
            return;
        }

        createLazyTick$idleInputTick = 0;
        int maxInterval = ServerConfig.getSawDelayMax();
        createLazyTick$inputSchedule = SawFrequencyFunction.onIdleProbe(createLazyTick$inputSchedule, maxInterval);
        int nextInterval = SawFrequencyFunction.nextInputProbeInterval(
                createLazyTick$inputSchedule, level.getGameTime(), maxInterval, 2);
        LazyTickLogic.setIntervalSafe(this, nextInterval);
    }

    @Inject(method = "start", remap = false, at = @At("HEAD"))
    public void createLazyTick$wakeOnInput(ItemStack inserted, CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;
        createLazyTick$wakeForInput();
    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/SawBlockEntity;getItemMovementVec()Lnet/minecraft/world/phys/Vec3;"), cancellable = true)
    public void createLazyTick$gateBlockedOutputRetry(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        // 到达此处代表 Create 已完成加工倒计时，正准备输出或抛出产物。
        // 尚未阻塞时第一次必须放行；失败后由 onOutputFail 逐步增加下一次重试间隔。
        if (inventory.remainingTime > 0) {
            createLazyTick$sawTick = 0;
            createLazyTick$allowNextOutputAttempt = true;
            return;
        }
        if (inventory.isEmpty()) {
            createLazyTick$sawTick = 0;
            return;
        }

        // A completed processing batch may export once immediately; only a confirmed unchanged
        // export enters the bounded retry policy below.
        if (createLazyTick$allowNextOutputAttempt) {
            createLazyTick$allowNextOutputAttempt = false;
            createLazyTick$sawTick = 0;
            createLazyTick$outputAttempted = true;
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
            createLazyTick$adaptiveSchedule = SawFrequencyFunction.onOutputSuccess(
                    createLazyTick$adaptiveSchedule, level.getGameTime(), ServerConfig.getSawDelayMax());
        }
        createLazyTick$sawTick = 0;
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
