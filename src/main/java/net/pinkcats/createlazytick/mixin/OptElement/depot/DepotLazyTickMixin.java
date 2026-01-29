package net.pinkcats.createlazytick.mixin.OptElement.depot;


import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = DepotBehaviour.class, remap = false)
public class DepotLazyTickMixin extends BlockEntityBehaviour {

    @Shadow
    Predicate<Direction> canFunnelsPullFrom;

    @Shadow
    ItemStackHandler processingOutputBuffer;

    @Shadow
    TransportedItemStack heldItem;

    @Shadow
    List<TransportedItemStack> incoming;

    @Shadow
    private boolean handleBeltFunnelOutput() {
        return false;
    }

    @Shadow
    TransportedItemStackHandlerBehaviour transportedHandler;

    @Shadow
    public static final BehaviourType<DepotBehaviour> TYPE = new BehaviourType<>();

    public DepotLazyTickMixin(SmartBlockEntity be) {
        super(be);
    }

    @Shadow
    protected boolean tick(TransportedItemStack heldItem) {
        return false;
    }

    @Shadow
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Unique
    private int createLazyTick$DepotDelayTick = 0;

    @Unique
    private void createLazyTick$applyBackoff() {
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this.blockEntity;

        int currentLazyTickInterval = control.createLazyTick$getLazyTickInterval();

        int newLazyTickInterval = LazyTickLogic.computeNextInterval(control, currentLazyTickInterval, ServerConfig.getDepotDelayMax());

        if (newLazyTickInterval != currentLazyTickInterval) {
            LazyTickLogic.setIntervalSafe(control, newLazyTickInterval);
        }
    }

    @Unique
    private void createLazyTick$resetDelayTick() {
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this.blockEntity;
        createLazyTick$DepotDelayTick = 0;

        LazyTickLogic.setIntervalSafe(control, 1);
    }

    @Inject(method = "tick*",at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyDepot()) {
            return;
        }

        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this.blockEntity;

        super.tick();

        NetworkSyncHelper.createLazyTick$syncPacketData(control, this.blockEntity.getLevel(),
                this.blockEntity.getBlockPos(), control.createLazyTick$getLazyTickInterval(), ServerConfig.getDepotDelayMax());

        Level world = blockEntity.getLevel();

        for (Iterator<TransportedItemStack> iterator = incoming.iterator(); iterator.hasNext();) {
            TransportedItemStack ts = iterator.next();
            boolean tick_res = tick(ts);
            //System.out.println(tick_res);

            if (!tick_res || world == null)
                continue;
            if (world.isClientSide && !blockEntity.isVirtual())
                continue;
            if (heldItem == null) {
                heldItem = ts;
            } else {
                if (!ItemHelper.canItemStackAmountsStack(heldItem.stack, ts.stack)) {
                    Vec3 vec = VecHelper.getCenterOf(blockEntity.getBlockPos());
                    Containers.dropItemStack(world, vec.x, vec.y + .5f, vec.z, ts.stack);
                } else {
                    heldItem.stack.grow(ts.stack.getCount());
                }
            }
            iterator.remove();
            blockEntity.notifyUpdate();
        }


        if (heldItem == null) {
            ci.cancel();
            return;
        }
        if (!tick(heldItem)) {
            ci.cancel();
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();

        if (world == null || world.isClientSide) {
            ci.cancel();
            return;
        }

        //tick emerge
        /*if (!world.isClientSide()) {
            System.out.println("Depot" + createLazyTick$DepotDelayTick + "  " + control.createLazyTick$getLazyTickInterval());
        }*/


        createLazyTick$DepotDelayTick++;
        if (createLazyTick$DepotDelayTick < control.createLazyTick$getLazyTickInterval()) {
            ci.cancel();
            return;
        }
        createLazyTick$DepotDelayTick = 0;


        if (handleBeltFunnelOutput()) {
            ci.cancel();
            return;
        }

        BeltProcessingBehaviour processingBehaviour =
                BlockEntityBehaviour.get(world, pos.above(2), BeltProcessingBehaviour.TYPE);
        if (processingBehaviour == null) {
            ci.cancel();
            return;
        }
        if (!heldItem.locked && BeltProcessingBehaviour.isBlocked(world, pos)) {
            ci.cancel();
            return;
        }



        ItemStack previousItem = heldItem.stack;
        boolean wasLocked = heldItem.locked;
        BeltProcessingBehaviour.ProcessingResult result = wasLocked ? processingBehaviour.handleHeldItem(heldItem, transportedHandler)
                : processingBehaviour.handleReceivedItem(heldItem, transportedHandler);






        if (result == BeltProcessingBehaviour.ProcessingResult.PASS) {
            createLazyTick$applyBackoff();
        }


        if (result == BeltProcessingBehaviour.ProcessingResult.HOLD) {
            createLazyTick$resetDelayTick();
        }

        if (heldItem == null || result == BeltProcessingBehaviour.ProcessingResult.REMOVE) {
            heldItem = null;
            blockEntity.sendData();
            {
                ci.cancel();
                return;
            }
        }
        heldItem.locked = result == BeltProcessingBehaviour.ProcessingResult.HOLD;
        if (heldItem.locked != wasLocked || !previousItem.equals(heldItem.stack, false)) {
            blockEntity.sendData();
        }
        ci.cancel();
    }


    @Inject(method = "handleBeltFunnelOutput",at=@At("HEAD" ),remap = false,cancellable = true)
    private void handleBeltFunnelOutput(CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyDepot()) {
            return;
        }

        BlockState funnel = getWorld().getBlockState(getPos().above());
        Direction funnelFacing = AbstractFunnelBlock.getFunnelFacing(funnel);
        if (funnelFacing == null || !canFunnelsPullFrom.test(funnelFacing.getOpposite())) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        for (int slot = 0; slot < processingOutputBuffer.getSlots(); slot++) {
            ItemStack previousItem = processingOutputBuffer.getStackInSlot(slot);
            if (previousItem.isEmpty())
                continue;

            ItemStack afterInsert = blockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE)
                    .tryExportingToBeltFunnel(previousItem, null, false);



            if (afterInsert == null) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
            if (previousItem.getCount() != afterInsert.getCount()) {
                processingOutputBuffer.setStackInSlot(slot, afterInsert);
                blockEntity.notifyUpdate();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        ItemStack previousItem = heldItem.stack;
        ItemStack afterInsert = blockEntity.getBehaviour(DirectBeltInputBehaviour.TYPE)
                .tryExportingToBeltFunnel(previousItem, null, false);


        if (afterInsert == null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        if (afterInsert.getCount() != 0) {
            createLazyTick$applyBackoff();
        } else {
            createLazyTick$resetDelayTick();
        }

        //System.out.println(afterInsert);

        //System.out.println("previousItem Output:"+previousItem);

        if (previousItem.getCount() != afterInsert.getCount()) {
            if (afterInsert.isEmpty())
                heldItem = null;
            else
                heldItem.stack = afterInsert;
            blockEntity.notifyUpdate();
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        cir.setReturnValue(false);
        cir.cancel();
    }

    @Unique
    public int createLazyTick$getCurrentDelayTick() {
        return ((ISmartBlockEntityControl) this.blockEntity).createLazyTick$getLazyTickInterval();
    }

    @Unique
    public int createLazyTick$getDepotDelayTick() {
        return createLazyTick$DepotDelayTick;
    }
}
