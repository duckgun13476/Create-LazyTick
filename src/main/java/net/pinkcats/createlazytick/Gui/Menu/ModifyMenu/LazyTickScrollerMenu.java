package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.menu.NutKineticMenu;

import java.util.Map;

/**
 * Example concrete subclass for the WhatIsThis menu.
 * Extend this class to implement per-menu behaviors.
 */
public class LazyTickScrollerMenu extends NutKineticMenu.NutItemMenu {

    public LazyTickScrollerMenu(Inventory inventory, int containerId, BlockPos pos, ResourceLocation menuId) {
        super(inventory, containerId, pos, menuId);
    }

    @Override
    protected void appendAutoSyncVariables(Map<String, Object> variables) {
        super.appendAutoSyncVariables(variables);
        variables.put("menuType", getMenuId().toString());
    }



}
