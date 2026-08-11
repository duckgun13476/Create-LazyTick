package net.pinkcats.createlazytick.mixin.OptElement.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A redstone-link network update walks every endpoint using the same pair of frequencies.
 * Create invokes it even when a neighbouring update leaves the transmitter strength unchanged.
 */
@Mixin(value = RedstoneLinkBlock.class, remap = false)
public abstract class RedstoneLinkSignalDedupMixin {

    @Redirect(
        method = "lambda$updateTransmittedSignal$1",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkBlockEntity;transmit(I)V")
    )
    private static void createLazyTick$transmitOnlyWhenChanged(RedstoneLinkBlockEntity blockEntity, int strength) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableRedstoneLinkSignalDedup()
                || blockEntity.getSignal() != strength)
            blockEntity.transmit(strength);
    }
}
