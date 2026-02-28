package net.pinkcats.createlazytick.Gui.Menu;

import net.pinkcats.NutUI.menu.extensions.NutMenuExtensionRegistry;
import net.pinkcats.createlazytick.Gui.Menu.ModifyMenu.LazyTickScrollerMenu;
import net.pinkcats.createlazytick.Gui.Menu.ModifyMenu.LazyTickScrollerScreen;

public final class MenuClientInit {
    private MenuClientInit() {
    }

    public static void registerScreens() {
        NutMenuExtensionRegistry.register(
                MenuInit.LazyTickScrollerBase,
                (id, inv, player, pos, menuId) -> new LazyTickScrollerMenu(inv, id, pos, menuId),
                LazyTickScrollerScreen::new
        );
    }
}
