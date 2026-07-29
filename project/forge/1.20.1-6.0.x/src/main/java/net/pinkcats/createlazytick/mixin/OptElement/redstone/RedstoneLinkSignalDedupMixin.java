package net.pinkcats.createlazytick.mixin.OptElement.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A redstone-link network update walks every endpoint using the same pair of frequencies.
 * Create invokes it even when a neighbouring update leaves the transmitter strength unchanged.
 */
@Mixin(value = RedstoneLinkBlockEntity.class, remap = false)
public abstract class RedstoneLinkSignalDedupMixin {

    @Shadow
    private int transmittedSignal;

    @Inject(method = "transmit", at = @At("HEAD"), cancellable = true)
    private void createLazyTick$skipUnchangedSignal(int strength, CallbackInfo ci) {
        if (transmittedSignal == strength)
            ci.cancel();
    }
}
