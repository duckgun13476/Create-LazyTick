package net.pinkcats.createlazytick.mixin.fluid;

import com.simibubi.create.content.fluids.FluidNetwork;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkTransferSpeedMixin {
    @Shadow
    private int transferSpeed;

    @Redirect(
            method = "tick",
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/fluids/FluidNetwork;transferSpeed:I", opcode = Opcodes.PUTFIELD),
            require = 1,
            remap = false
    )
    private void createLazyTick$scaleTransferSpeed(FluidNetwork self, int original) {
        this.transferSpeed = ServerConfig.getEnableLazyTick() && ServerConfig.getEnableLazyFluid()
                ? (int) Math.max(1, original * ServerConfig.getFluidDelayMax())
                : original;
    }
}
