package net.pinkcats.createlazytick.mixin.compat.vintageimprovements;

import net.pinkcats.createlazytick.bridge.Basin.IBasinLazyTickBypass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Vintage Improvements keeps pressurizing secondary fluids in the compressor,
 * outside BasinStateSnapshot. The marker lets the shared basin optimization
 * leave this machine on Create's normal update path.
 */
@Pseudo
@Mixin(targets = "com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity", remap = false)
public abstract class VacuumChamberBasinBypassMixin implements IBasinLazyTickBypass {
}
