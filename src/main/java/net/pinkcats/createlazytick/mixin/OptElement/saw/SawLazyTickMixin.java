package net.pinkcats.createlazytick.mixin.OptElement.saw;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickLogic;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
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

    @Unique
    private int createLazyTick$sawTick = 0;

    @Unique
    private boolean createLazyTick$inventoryChanged = false; //脏标记

    @Unique
    private void createLazyTick$resetDelayTick() {
        createLazyTick$sawTick = 0;
        LazyTickLogic.setIntervalSafe(this, 1);
    }

    @Unique
    private void createLazyTick$applyBackoff() {
        createLazyTick$sawTick = 0;
        int currentInterval = this.createLazyTick$getLazyTickInterval();
        int newInterval = LazyTickLogic.computeNextInterval(this, currentInterval, ServerConfig.saw_delay_max);

        if (newInterval != currentInterval) {
            LazyTickLogic.setIntervalSafe(this, newInterval);
        }
    }

    public SawLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick_Head(CallbackInfo ci) {
        if (!ServerConfig.enable_lazy_tick || !ServerConfig.enable_lazy_saw) return;
        if (level == null || level.isClientSide) return;

        NetworkSyncHelper.createLazyTick$syncPacketData(this,
                this.level, this.worldPosition, this.createLazyTick$getLazyTickInterval(), ServerConfig.saw_delay_max);

        createLazyTick$inventoryChanged = false;
    }

    @Inject(method = "tick", remap = false, at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/SawBlockEntity;getItemMovementVec()Lnet/minecraft/world/phys/Vec3;"), cancellable = true)
    public void optimizedTick(CallbackInfo ci) {
        if (!ServerConfig.enable_lazy_tick || !ServerConfig.enable_lazy_saw) return;
        if (level == null || level.isClientSide) return;

        /*if(!level.isClientSide()) {
            System.out.println("saw:" + createLazyTick$sawTick + "|" + this.createLazyTick$getLazyTickInterval());
        }*/

        // 如果正在加工(remainingTime > 0) 或 空闲(Empty),不拦截
        if (inventory.remainingTime > 0 || inventory.isEmpty()) {
            createLazyTick$resetDelayTick();
            return;
        }

        createLazyTick$sawTick++;

        // Tick < Interval -> Cancel
        if (createLazyTick$sawTick < this.createLazyTick$getLazyTickInterval()) {
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
        if (!ServerConfig.enable_lazy_tick || !ServerConfig.enable_lazy_saw) return;

        if (level == null || level.isClientSide) return;
        createLazyTick$inventoryChanged = true;
        createLazyTick$resetDelayTick();
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void onOutputFail(CallbackInfo ci) {
        if (!ServerConfig.enable_lazy_tick || !ServerConfig.enable_lazy_saw) return;

        if (level == null || level.isClientSide) return;

        if (inventory.remainingTime == 0 && !inventory.isEmpty() && !createLazyTick$inventoryChanged) {
            createLazyTick$applyBackoff();
        }
    }

    @Inject(method = "addBehaviours", at = @At("RETURN"), remap = false)
    private void lazytick$addScrollBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        LazyTickScrollBehaviour.addTo(this, behaviours);
    }
}