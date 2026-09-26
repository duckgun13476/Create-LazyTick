package net.pinkcats.createlazytick.fabric;

import net.fabricmc.api.ModInitializer;

public final class CreateLazyTickFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        net.pinkcats.createlazytick.CreateLazyTick.initialize();
    }
}
