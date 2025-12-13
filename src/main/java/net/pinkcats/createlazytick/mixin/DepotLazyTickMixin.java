package net.pinkcats.createlazytick.mixin;


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
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.pinkcats.createlazytick.Config;
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

import static net.pinkcats.createlazytick.Config.depot_delay_max;

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



    @Inject(method = "tick*",at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {
        if (!Config.enable_lazy_tick || !Config.enable_lazy_depot) {
            return;
        }

        super.tick();


        Level world = blockEntity.getLevel();
        for (Iterator<TransportedItemStack> iterator = incoming.iterator(); iterator.hasNext();) {
            TransportedItemStack ts = iterator.next();
            boolean tick_res = tick(ts);
            //System.out.println(tick_res);

            if (!tick_res)
                continue;
            if (world.isClientSide && !blockEntity.isVirtual())
                continue;
            if (heldItem == null) {
                heldItem = ts;
            } else {
                if (!ItemHelper.canItemStackAmountsStack(heldItem.stack, ts.stack)) {
                    Vec3 vec = VecHelper.getCenterOf(blockEntity.getBlockPos());
                    Containers.dropItemStack(blockEntity.getLevel(), vec.x, vec.y + .5f, vec.z, ts.stack);
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

        if (world.isClientSide) {
            ci.cancel();
            return;
        }
        if (handleBeltFunnelOutput()) {
            ci.cancel();
            return;
        }

        //tick emerge
        //System.out.println("Depot" + createLazyTick$CurrentDelayTick + "  " + createLazyTick$DepotDelayTick);


        if (createLazyTick$CurrentDelayTick > createLazyTick$DepotDelayTick) {
           createLazyTick$CurrentDelayTick = 0;
        } else {

            createLazyTick$CurrentDelayTick = createLazyTick$CurrentDelayTick + 1;
            if (createLazyTick$CurrentDelayTick>5){

                ci.cancel();
                return;
            }
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






        if (result == BeltProcessingBehaviour.ProcessingResult.PASS)
            if (createLazyTick$DepotDelayTick < depot_delay_max)
                createLazyTick$DepotDelayTick = createLazyTick$DepotDelayTick + Math.max(createLazyTick$CurrentDelayTick / 10, 1);

        if (result == BeltProcessingBehaviour.ProcessingResult.HOLD) {
            createLazyTick$DepotDelayTick = 0;
            createLazyTick$CurrentDelayTick =0;
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

    @Unique
    private int createLazyTick$DepotDelayTick = 0;
    @Unique
    private int createLazyTick$CurrentDelayTick = 0;


    @Inject(method = "handleBeltFunnelOutput",at=@At("HEAD" ),remap = false,cancellable = true)
    private void handleBeltFunnelOutput(CallbackInfoReturnable<Boolean> cir) {
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

            //System.out.println(previousItem);

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


        //System.out.println("previousItem Output:"+previousItem);
        if (afterInsert == null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
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
    public int getDepotDelayTick() {
        return createLazyTick$DepotDelayTick;
    }

    @Unique
    public int getCurrentDelayTick() {
        return createLazyTick$CurrentDelayTick;
    }
}
