package net.pinkcats.createlazytick.mixin.fluid;

import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.fluids.PipeConnection;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(value = FluidNetwork.class, remap = false)
public interface FluidNetworkAccessor {
    @Accessor("source")
    LazyOptional<IFluidHandler> getSource();



}
