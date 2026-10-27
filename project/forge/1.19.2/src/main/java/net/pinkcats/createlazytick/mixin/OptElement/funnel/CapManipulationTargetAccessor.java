package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CapManipulationBehaviourBase.class, remap = false)
public interface CapManipulationTargetAccessor {
    @Accessor("target")
    CapManipulationBehaviourBase.InterfaceProvider createLazyTick$getTargetProvider();
}
