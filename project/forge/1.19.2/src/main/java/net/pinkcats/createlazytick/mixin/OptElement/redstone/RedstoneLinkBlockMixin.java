package net.pinkcats.createlazytick.mixin.OptElement.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import net.pinkcats.createlazytick.redstone.RedstoneLinkRefreshAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Limit only Create's ordinary block-update transmission, not explicit callers. */
@Mixin(RedstoneLinkBlock.class)
public abstract class RedstoneLinkBlockMixin {

    @Redirect(
            method = "lambda$updateTransmittedSignal$0",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkBlockEntity;transmit(I)V"),
            remap = false
    )
    private static void createLazyTick$limitUnchangedBlockUpdate(RedstoneLinkBlockEntity blockEntity, int signal) {
        if (((RedstoneLinkRefreshAccess) blockEntity).createLazyTick$allowRegularTransmission(signal)) {
            blockEntity.transmit(signal);
        }
    }
}
