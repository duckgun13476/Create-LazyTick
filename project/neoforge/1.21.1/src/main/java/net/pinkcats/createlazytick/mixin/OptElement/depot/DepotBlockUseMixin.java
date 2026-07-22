package net.pinkcats.createlazytick.mixin.OptElement.depot;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.pinkcats.createlazytick.helper.LazyTickScrollerOpenHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DepotBlock.class)
public class DepotBlockUseMixin {

    @Inject(method = "useItemOn", remap = false, at = @At("HEAD"), cancellable = true)
    private void createLazyTick$openTestUiOnLowerHalf(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                      InteractionHand hand, BlockHitResult hit,
                                                      CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (LazyTickScrollerOpenHelper.tryOpen(level, pos, player, hand, hit)) {
            cir.setReturnValue(ItemInteractionResult.CONSUME);
        }
    }
}
