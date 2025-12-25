package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FlowSource;
import net.createmod.catnip.math.BlockFace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FlowSource.class,remap = false)
public interface FluidAccessor {

    @Accessor("location")
    BlockFace getLocation();

}
