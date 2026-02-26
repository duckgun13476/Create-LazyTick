package net.pinkcats.createlazytick.mixin.OptElement.depot;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.pinkcats.NutUI.menu.extensions.NutMenuExtensionRegistry;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.pinkcats.NutUI.menu.architect.Helper.MenuHelper.CreateNutMenu;
import static net.pinkcats.createlazytick.Gui.Menu.MenuInit.LazyTickScrollerBase;

@Mixin(value = DepotBlock.class, remap = false)
public class DepotBlockUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$openTestUiOnLowerHalf(BlockState state, Level level, BlockPos pos, Player player,
                                                      InteractionHand hand, BlockHitResult hit,
                                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        if (player.getMainHandItem().getItem() != LazyTickItem.CLOCK.get()) {
            return;
        }

        double localY = hit.getLocation().y - pos.getY();
        if (localY >= 0.5D) {
            return;
        }

        CreateNutMenu(player, pos, LazyTickScrollerBase, (id, inv, p, menuPos, menuId) ->
                NutMenuExtensionRegistry.createMenu(inv, id, p, menuPos, menuId));
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
