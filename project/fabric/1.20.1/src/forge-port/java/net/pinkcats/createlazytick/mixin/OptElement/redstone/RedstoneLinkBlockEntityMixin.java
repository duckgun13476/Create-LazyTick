package net.pinkcats.createlazytick.mixin.OptElement.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoid a full wireless network traversal when the transmitter strength did not change. */
@Mixin(RedstoneLinkBlockEntity.class)
public abstract class RedstoneLinkBlockEntityMixin {

    @Shadow(remap = false)
    private int transmittedSignal;

    @Inject(method = "transmit", at = @At("HEAD"), cancellable = true, remap = false)
    private void lazytick$skipUnchangedTransmission(int signal, CallbackInfo ci) {
        if (transmittedSignal == signal) {
            ci.cancel();
        }
    }
}
