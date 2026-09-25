package net.pinkcats.createlazytick.mixin.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skip an unchanged transmitter value instead of notifying its whole wireless network. */
@Mixin(value = RedstoneLinkBlockEntity.class, remap = false)
public abstract class RedstoneLinkBlockEntityMixin {
    @Shadow(remap = false) private int transmittedSignal;

    @Inject(method = "transmit", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$skipUnchangedTransmission(int signal, CallbackInfo ci) {
        if (transmittedSignal == signal) ci.cancel();
    }
}
