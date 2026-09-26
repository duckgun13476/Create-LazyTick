package net.pinkcats.createlazytick.mixin.OptElement.factorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryGaugeLazyTickMixin {

    @Shadow(remap = false) public boolean satisfied;
    @Shadow(remap = false) public boolean promisedSatisfied;
    @Shadow(remap = false) public boolean waitingForNetwork;
    @Shadow(remap = false) public boolean forceClearPromises;
    @Shadow(remap = false) public boolean redstonePowered;
    @Shadow(remap = false) public boolean active;
    @Shadow(remap = false) private int lastReportedUnloadedLinks;
    @Shadow(remap = false) public abstract FactoryPanelBlockEntity panelBE();
    @Shadow(remap = false) public Map<?, ?> targetedBy;
    @Shadow(remap = false) public Map<?, ?> targetedByLinks;

    @Unique private int createLazyTick$stableInterval = 1;
    @Unique private int createLazyTick$skippedChecks;

    @Inject(method = "tickStorageMonitor", at = @At("HEAD"), cancellable = true, remap = false)
    private void createLazyTick$skipRepeatedStableMonitor(CallbackInfo ci) {
        if (!createLazyTick$canDelayMonitor()) {
            createLazyTick$resetMonitorDelay();
            return;
        }

        if (createLazyTick$skippedChecks > 0) {
            createLazyTick$skippedChecks--;
            ci.cancel();
        }
    }

    @Inject(method = "tickStorageMonitor", at = @At("RETURN"), remap = false)
    private void createLazyTick$backOffAfterStableMonitor(CallbackInfo ci) {
        if (!createLazyTick$canDelayMonitor()) {
            createLazyTick$resetMonitorDelay();
            return;
        }

        int maxInterval = ServerConfig.getFactoryGaugeStableDelayMax();
        createLazyTick$stableInterval = createLazyTick$stableInterval >= maxInterval - createLazyTick$stableInterval
            ? maxInterval
            : createLazyTick$stableInterval * 2;
        createLazyTick$skippedChecks = createLazyTick$stableInterval - 1;
    }

    @Unique
    private boolean createLazyTick$canDelayMonitor() {
        return ServerConfig.getEnableLazyTick()
            && ServerConfig.getEnableLazyFactoryGauge()
            && active
            && !panelBE().restocker
            && !forceClearPromises
            && !redstonePowered
            && !waitingForNetwork
            && lastReportedUnloadedLinks == 0
            && satisfied
            && promisedSatisfied
            && targetedBy.isEmpty()
            && targetedByLinks.isEmpty()
            && !((FilteringBehaviour) (Object) this).getFilter().isEmpty();
    }

    @Unique
    private void createLazyTick$resetMonitorDelay() {
        createLazyTick$stableInterval = 1;
        createLazyTick$skippedChecks = 0;
    }
}
