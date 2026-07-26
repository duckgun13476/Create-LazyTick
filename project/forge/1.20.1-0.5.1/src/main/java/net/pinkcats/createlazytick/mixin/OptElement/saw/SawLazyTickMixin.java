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

import java.util.List;

@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawLazyTickMixin extends KineticBlockEntity implements ISmartBlockEntityControl {

    @Shadow(remap = false)
    public ProcessingInventory inventory;

    @Shadow(remap = false)
    protected abstract boolean canProcess();

    @Unique
    private int createLazyTick$sawTick = 0;

    @Unique
    private boolean createLazyTick$inventoryChanged = false; //脏标记

    @Unique
    private boolean createLazyTick$allowNextOutputAttempt = true;

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
        int currentInterval = this.createLazyTick$getCurrentSuperTick();
        int newInterval = LazyTickLogic.computeNextInterval(this, currentInterval, ServerConfig.getSawDelayMax());

        if (newInterval != currentInterval) {
            LazyTickLogic.setIntervalSafe(this, newInterval);
        }
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
    }

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
    public void optimizedTick(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;
        if (level == null || level.isClientSide) return;

        /*if(!level.isClientSide()) {
            System.out.println("saw:" + createLazyTick$sawTick + "|" + this.createLazyTick$getLazyTickInterval());
        }*/

        // 加工保持全速，但保留空闲学习到的间隔；每个完成批次的首次导出必定立即放行。
        if (inventory.remainingTime > 0) {
            createLazyTick$sawTick = 0;
            createLazyTick$allowNextOutputAttempt = true;
            return;
        }
        if (inventory.isEmpty()) {
            createLazyTick$sawTick = 0;
            return;
        }
        if (createLazyTick$allowNextOutputAttempt) {
            createLazyTick$allowNextOutputAttempt = false;
            createLazyTick$sawTick = 0;
            return;
        }

        createLazyTick$sawTick++;

        // Tick < Interval -> Cancel
        if (createLazyTick$sawTick < this.createLazyTick$getCurrentSuperTick()) {
            ci.cancel();
        } else {
            // reset timer and try Logic
            createLazyTick$sawTick = 0;
        }
    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/recipe/ProcessingInventory;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"))
    public void onInventoryChange(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;

        if (level == null || level.isClientSide) return;
        createLazyTick$inventoryChanged = true;
        createLazyTick$sawTick = 0;
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void onOutputFail(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazySaw()) return;

        if (level == null || level.isClientSide) return;

        if (inventory.remainingTime == 0 && !inventory.isEmpty() && !createLazyTick$inventoryChanged) {
            createLazyTick$applyBackoff();
        }
    }

}
