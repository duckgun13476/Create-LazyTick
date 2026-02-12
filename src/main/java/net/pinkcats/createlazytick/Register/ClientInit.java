package net.pinkcats.createlazytick.Register;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.config.ui.BaseConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.config.ServerConfig;

import static net.pinkcats.createlazytick.CreateLazyTick.getModLoadingContextViaReflection;

@Mod.EventBusSubscriber(modid = CreateLazyTick.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {

        if (event.getTab() == AllCreativeModeTabs.BASE_CREATIVE_TAB.get()) {
            event.accept(LazyTickItem.CLOCK.get());
        }
    }

    public static void initClient() {
        ModLoadingContext modLoadingContext = getModLoadingContextViaReflection();
        modLoadingContext.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, screen) -> new BaseConfigScreen(screen, CreateLazyTick.MODID)
                                .withSpecs(ClientConfig.SPEC, null, ServerConfig.SPEC)
                )
        );
    }
}
