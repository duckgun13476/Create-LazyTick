package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.pinkcats.createlazytick.config.ServerConfig;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = FluidNetwork.class, remap = false)
    public abstract class FluidNetworkTransferSpeedMixin {

        @Shadow
        int transferSpeed;

        @Shadow
        ICapabilityProvider<IFluidHandler> source;

        @Shadow
        public abstract void reset();

        @Unique
        private static final String CLT$INVALID_CACHE_MSG = "invalid cache";

        @Redirect(
                method = "tick",
                at = @At(
                        value = "FIELD",
                        target = "Lcom/simibubi/create/content/fluids/FluidNetwork;transferSpeed:I",
                        opcode = Opcodes.PUTFIELD
                ),
                require = 0
        )
        private void createlazytick$scaleTransferSpeed(FluidNetwork self, int original) {
            float mult = ServerConfig.getFluidDelayMax();
            this.transferSpeed = (int) Math.max(1, original * mult);
        }

        @WrapOperation(
                method = "tick",
                at = @At(
                        value = "INVOKE",
                        target = "Lcom/simibubi/create/foundation/ICapabilityProvider;getCapability()Ljava/lang/Object;"
                ),
                require = 0
        )
        private Object createlazytick$safeGetCapability(ICapabilityProvider<?> provider, Operation<Object> original) {
            try {
                return original.call(provider);
            } catch (IllegalStateException ex) {
                // Silent self-heal for migrated worlds: skip this cycle and let the network rebuild capability caches.
                String msg = ex.getMessage();
                if (msg != null && msg.toLowerCase().contains(CLT$INVALID_CACHE_MSG)) {
                    if (provider == this.source) {
                        this.source = null;
                    }
                    this.reset();
                    return null;
                }
                throw ex;
            }
        }
    }



