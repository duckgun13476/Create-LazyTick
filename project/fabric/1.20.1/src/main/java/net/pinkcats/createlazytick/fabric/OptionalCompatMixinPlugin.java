package net.pinkcats.createlazytick.fabric;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Register optional integrations only when their exact target class is present. */
public final class OptionalCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String VINTAGE_CHAMBER =
            "com/negodya1/vintageimprovements/content/kinetics/vacuum_chamber/VacuumChamberBlockEntity.class";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return getClass().getClassLoader().getResource(VINTAGE_CHAMBER) == null
                ? List.of()
                : List.of("compat.vintageimprovements.VacuumChamberBasinBypassMixin");
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
