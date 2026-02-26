package net.pinkcats.NutUI.menu;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.pinkcats.NutUI.menu.extensions.NutMenuScreenRouter;

import static net.pinkcats.NutUI.menu.NutKineticMenu.ItemMenuRegiste;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientScreen {


    @SubscribeEvent
    public static void registerScreen(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ItemMenuRegiste.get(), NutMenuScreenRouter::create));
    }


}
