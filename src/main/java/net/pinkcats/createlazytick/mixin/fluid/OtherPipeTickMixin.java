package net.pinkcats.createlazytick.mixin.fluid;

import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraftforge.fluids.FluidStack;
import net.pinkcats.createlazytick.mixin.itemdrain.ItemDrainAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;

@Mixin(value = FlowSource.OtherPipe.class,remap = false)
public class OtherPipeTickMixin {


    @Shadow
    WeakReference<FluidTransportBehaviour> cached;

    @Inject(method = "provideFluid" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void provideFluid(Predicate<FluidStack> extractionPredicate, CallbackInfoReturnable<FluidStack> cir) {
        FluidAccessor fluidAccessor = (FluidAccessor) this;
        BlockFace location = fluidAccessor.getLocation();

        if (cached == null || cached.get() == null) {
            cir.setReturnValue(FluidStack.EMPTY);
            return;
        }

        FluidTransportBehaviour behaviour = cached.get();
        FluidStack providedOutwardFluid = behaviour.getProvidedOutwardFluid(location.getOppositeFace());

        FluidStack K = extractionPredicate.test(providedOutwardFluid) ? providedOutwardFluid : FluidStack.EMPTY;
        System.out.println("Override"+K.getAmount());
        cir.setReturnValue(K);
    }
}
