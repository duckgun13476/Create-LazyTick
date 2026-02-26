package net.pinkcats.NutUI.menu.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import net.pinkcats.NutUI.menu.Nutprovider;
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified registry for custom menu factories, screen factories and menu definitions.
 */
public final class NutMenuExtensionRegistry {

    private static final Map<ResourceLocation, Nutprovider.MenuBuilder> MENU_FACTORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ScreenBuilder> SCREEN_FACTORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, NutMenuInfo.data> MENU_DEFINITIONS = new LinkedHashMap<>();
    private static volatile boolean menusDefined = false;

    private NutMenuExtensionRegistry() {
    }

    public static void register(ResourceLocation menuId, Nutprovider.MenuBuilder menuBuilder) {
        register(menuId, menuBuilder, (menu, inv, title) -> new NutKineticScreen(menu, inv, title));
    }

    public static void register(ResourceLocation menuId, Nutprovider.MenuBuilder menuBuilder, ScreenBuilder screenBuilder) {
        if (menuId == null || menuBuilder == null) {
            return;
        }
        MENU_FACTORIES.put(menuId, menuBuilder);
        if (screenBuilder != null) {
            SCREEN_FACTORIES.put(menuId, screenBuilder);
        }
    }

    public static void registerEasyMenu(ResourceLocation menuId, ResourceLocation texture,
                                        int x, int y, Integer playerInventoryX, Integer playerInventoryY,
                                        Nutprovider.MenuBuilder menuBuilder, ScreenBuilder screenBuilder) {
        menusDefined = false;
        if (playerInventoryX == null || playerInventoryY == null) {
            MENU_DEFINITIONS.put(menuId, NutMenuInfo.data.EasyMenu(menuId, texture, x, y));
        } else {
            MENU_DEFINITIONS.put(menuId, NutMenuInfo.data.EasyMenu(menuId, texture, x, y, playerInventoryX, playerInventoryY));
        }
        register(menuId, menuBuilder, screenBuilder);
    }

    public static void defineRegisteredMenus() {
        if (menusDefined) {
            return;
        }
        for (NutMenuInfo.data def : MENU_DEFINITIONS.values()) {
            NutMenuInfo.define(def);
        }
        menusDefined = true;
    }

    public static NutKineticMenu.NutItemMenu createMenu(Inventory inventory, int containerId, Player player,
                                                         BlockPos pos, ResourceLocation menuId) {
        Nutprovider.MenuBuilder menuBuilder = MENU_FACTORIES.get(menuId);
        if (menuBuilder != null) {
            return (NutKineticMenu.NutItemMenu) menuBuilder.create(containerId, inventory, player, pos, menuId);
        }
        return new NutKineticMenu.NutItemMenu(inventory, containerId, pos, menuId);
    }

    public static NutKineticScreen createScreen(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title) {
        ScreenBuilder screenBuilder = SCREEN_FACTORIES.get(menu.getMenuId());
        if (screenBuilder != null) {
            return screenBuilder.create(menu, inventory, title);
        }
        return new NutKineticScreen(menu, inventory, title);
    }

    @FunctionalInterface
    public interface ScreenBuilder {
        NutKineticScreen create(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title);
    }
}
