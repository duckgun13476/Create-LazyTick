package net.pinkcats.createlazytick.mixin.OptElement.fluid;

import com.simibubi.create.content.fluids.FluidNetwork;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FluidNetwork.class, remap = false)
public interface FluidNetworkAccessor {
    @Accessor("source")
    LazyOptional<IFluidHandler> getSource();



}
