package net.pinkcats.createlazytick.mixin.fluid;


import com.simibubi.create.content.fluids.FlowSource;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = FlowSource.class,remap = false)
public class FlowSourceTickMixin {


    @Shadow
    private static final LazyOptional<IFluidHandler> EMPTY = LazyOptional.empty();



    @Shadow
    public LazyOptional<IFluidHandler> provideHandler() {
        return EMPTY;
    }


    @Inject(method = "provideFluid" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void provideFluid(Predicate<FluidStack> extractionPredicate, CallbackInfoReturnable<FluidStack> cir) {
        IFluidHandler tank = provideHandler().orElse(null);
        if (tank == null) {
            cir.setReturnValue(FluidStack.EMPTY);
            return;
        }
        int drain = 3;

        FluidStack immediateFluid = tank.drain(4, IFluidHandler.FluidAction.SIMULATE);
        if (extractionPredicate.test(immediateFluid)) {
            cir.setReturnValue(immediateFluid);
            System.out.println("Normal-"+immediateFluid.getAmount()); //flowing
            return;
        }

        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack contained = tank.getFluidInTank(i);
            if (contained.isEmpty())
                continue;
            if (!extractionPredicate.test(contained))
                continue;
            FluidStack toExtract = contained.copy();
            toExtract.setAmount(1);
            cir.setReturnValue(tank.drain(toExtract, IFluidHandler.FluidAction.SIMULATE));
            System.out.println("Normal+"+tank.drain(toExtract, IFluidHandler.FluidAction.SIMULATE).getAmount());
            return;
        }
        cir.setReturnValue(FluidStack.EMPTY);
    }





}
