package net.pinkcats.createlazytick.Register;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;

public class ClientInit {

    public static void initClient(ModContainer modContainer) {
        CreateLazyTick.LOGGER.info("[CreateLazyTick][ClientInit] registering client hooks");
        GogglesItem.addIsWearingPredicate(player -> {
            boolean result = player != null && (
                    player.getMainHandItem().getItem() == LazyTickItem.CLOCK.get()
                            || player.getOffhandItem().getItem() == LazyTickItem.CLOCK.get()
            );
            if (result && ClientConfig.enableDepotDebug()) {
                LazyTickDepotDebug.log(Minecraft.getInstance(), "goggles_predicate",
                        "clockPredicate=true, mainHand=" + player.getMainHandItem()
                                + ", offHand=" + player.getOffhandItem());
            }
            return result;
        });
        CreateLazyTick.LOGGER.info("[CreateLazyTick][ClientInit] goggles predicate registered");

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ModContainer container, Screen screen) -> new BaseConfigScreen(screen, CreateLazyTick.MODID)
                        .withSpecs(ClientConfig.SPEC, null, ServerConfig.SPEC)
        );
        CreateLazyTick.LOGGER.info("[CreateLazyTick][ClientInit] config screen extension registered");
    }
}
