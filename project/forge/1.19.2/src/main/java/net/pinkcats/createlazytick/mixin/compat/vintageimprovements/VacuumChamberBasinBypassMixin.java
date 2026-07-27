package net.pinkcats.createlazytick.mixin.compat.vintageimprovements;

import net.pinkcats.createlazytick.bridge.Basin.IBasinLazyTickBypass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity", remap = false)
public abstract class VacuumChamberBasinBypassMixin implements IBasinLazyTickBypass {
}
