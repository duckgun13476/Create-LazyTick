package net.pinkcats.NutUI.menu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.pinkcats.NutUI.menu.extensions.NutMenuScreenRouter;

import static net.pinkcats.NutUI.menu.NutKineticMenu.ItemMenuRegiste;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientScreen {

    @SubscribeEvent
    public static void registerScreen(RegisterMenuScreensEvent event) {
        event.register(ItemMenuRegiste.get(), NutMenuScreenRouter::create);
    }

}
