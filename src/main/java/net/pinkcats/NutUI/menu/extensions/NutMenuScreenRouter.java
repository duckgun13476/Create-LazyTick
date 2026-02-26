package net.pinkcats.NutUI.menu.extensions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;

/**
 * Routes menus to custom screens by menuId.
 */
public final class NutMenuScreenRouter {

    private NutMenuScreenRouter() {
    }

    public static NutKineticScreen create(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title) {
        return NutMenuExtensionRegistry.createScreen(menu, inventory, title);
    }
}
