package net.pinkcats.NutUI.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.pinkcats.NutUI.menu.extensions.NutMenuScreenRouter;

@Environment(EnvType.CLIENT)
public final class ClientScreen {
    private ClientScreen() {}

    public static void registerScreen() {
        MenuScreens.register(NutKineticMenu.ItemMenuRegiste.get(), NutMenuScreenRouter::create);
    }
}
