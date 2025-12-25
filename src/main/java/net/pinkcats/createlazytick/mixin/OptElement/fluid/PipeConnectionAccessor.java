package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.PipeConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(value = PipeConnection.class, remap = false)
public interface PipeConnectionAccessor {


    @Accessor("source")
    Optional<FlowSource> getSource();


    @Accessor("flow")
    Optional<PipeConnection.Flow> getFlow() ;


}
