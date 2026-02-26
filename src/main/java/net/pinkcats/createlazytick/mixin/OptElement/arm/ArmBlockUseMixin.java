package net.pinkcats.createlazytick.mixin.OptElement.arm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.pinkcats.createlazytick.helper.LazyTickScrollerOpenHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArmBlock.class, remap = false)
public class ArmBlockUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$openScrollerUi(BlockState state, Level level, BlockPos pos, Player player,
                                                InteractionHand hand, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (LazyTickScrollerOpenHelper.tryOpen(level, pos, player, hand, hit)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
