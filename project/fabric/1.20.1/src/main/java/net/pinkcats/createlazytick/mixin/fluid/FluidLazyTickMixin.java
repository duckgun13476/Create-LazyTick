package net.pinkcats.createlazytick.mixin.fluid;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidTransportBehaviour.class, remap = false)
public class FluidLazyTickMixin {
    @Unique
    private int createLazyTick$pipeTick;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$tick(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFluid()) {
            return;
        }
        if (++createLazyTick$pipeTick < ServerConfig.getFluidDelayMax()) {
            ci.cancel();
            return;
        }
        createLazyTick$pipeTick = 0;
    }
}
