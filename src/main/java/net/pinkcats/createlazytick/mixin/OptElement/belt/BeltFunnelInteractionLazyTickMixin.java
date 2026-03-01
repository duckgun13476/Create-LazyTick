package net.pinkcats.createlazytick.mixin.OptElement.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.BeltFunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeltFunnelInteractionHandler.class)
public class BeltFunnelInteractionLazyTickMixin {


    @Inject(method ="checkForFunnels" ,at=@At("HEAD" ),cancellable = true,remap = false)
    private static void checkForFunnels(BeltInventory beltInventory, TransportedItemStack currentItem, float nextOffset, CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyBelt()) {
            return;
        }
        BeltInventoryAccessor accessor = (BeltInventoryAccessor) beltInventory;
        boolean beltMovementPositive = accessor.getBeltMovementPositive();
        BeltBlockEntity beltInterface = accessor.getBelt();


        int firstUpcomingSegment = (int) Math.floor(currentItem.beltPosition);
        int step = beltMovementPositive ? 1 : -1;
        firstUpcomingSegment = Mth.clamp(firstUpcomingSegment, 0, beltInterface.beltLength - 1);

        for (int segment = firstUpcomingSegment; beltMovementPositive ? segment <= nextOffset
                : segment + 1 >= nextOffset; segment += step) {
            BlockPos funnelPos = BeltHelper.getPositionForOffset(beltInterface, segment)
                    .above();
            Level world = beltInterface.getLevel();
            BlockState funnelState = world.getBlockState(funnelPos);
            if (!(funnelState.getBlock() instanceof BeltFunnelBlock))
                continue;
            Direction funnelFacing = funnelState.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
            Direction movementFacing = beltInterface.getMovementFacing();
            boolean blocking = funnelFacing == movementFacing.getOpposite();
            if (funnelFacing == movementFacing)
                continue;
            if (funnelState.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.PUSHING)
                continue;

            float funnelEntry = segment + .5f;
            if (funnelState.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED)
                funnelEntry += .499f * (beltMovementPositive ? -1 : 1);
            boolean hasCrossed = nextOffset > funnelEntry && beltMovementPositive
                    || nextOffset < funnelEntry && !beltMovementPositive;
            if (!hasCrossed) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
            if (blocking)
                currentItem.beltPosition = funnelEntry;

            if (world.isClientSide || funnelState.getOptionalValue(BeltFunnelBlock.POWERED).orElse(false))
                if (blocking) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                else
                    continue;

            BlockEntity be = world.getBlockEntity(funnelPos);
            if (!(be instanceof FunnelBlockEntity funnelBE)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            InvManipulationBehaviour inserting = funnelBE.getBehaviour(InvManipulationBehaviour.TYPE);
            FilteringBehaviour filtering = funnelBE.getBehaviour(FilteringBehaviour.TYPE);

            if (inserting == null || filtering != null && !filtering.test(currentItem.stack))
                if (blocking) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                else
                    continue;


            if (beltInterface.invVersionTracker.stillWaiting(inserting))
                continue;

            int amountToExtract = funnelBE.getAmountToExtract();
            ItemHelper.ExtractionCountMode modeToExtract = funnelBE.getModeToExtract();

            ItemStack toInsert = currentItem.stack.copy();
            if (amountToExtract > toInsert.getCount() && modeToExtract != ItemHelper.ExtractionCountMode.UPTO)
                if (blocking) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
                else
                    continue;

            if (amountToExtract != -1 && modeToExtract != ItemHelper.ExtractionCountMode.UPTO) {
                toInsert.setCount(Math.min(amountToExtract, toInsert.getCount()));
                ItemStack remainder = inserting.simulate()
                        .insert(toInsert);
                if (!remainder.isEmpty())
                    if (blocking) {
                        cir.setReturnValue(true);
                        cir.cancel();
                        return;
                    }
                    else
                        continue;
                else
                    beltInterface.invVersionTracker.awaitNewVersion(inserting);
            }

            ItemStack remainder = inserting.insert(toInsert);
            if (ItemStack.matches(toInsert, remainder)) {
                beltInterface.invVersionTracker.awaitNewVersion(inserting);
                if (blocking) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
                else
                    continue;
            }

            int notFilled = currentItem.stack.getCount() - toInsert.getCount();
            if (!remainder.isEmpty()) {
                remainder.grow(notFilled);
            } else if (notFilled > 0)
                remainder = currentItem.stack.copyWithCount(notFilled);

            funnelBE.flap(true);
            funnelBE.onTransfer(toInsert);
            currentItem.stack = remainder;
            beltInterface.notifyUpdate();
            if (blocking) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
        cir.setReturnValue(false);
        cir.cancel();
    }
}
