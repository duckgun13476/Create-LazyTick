package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;

import java.util.Map;

/**
 * Example concrete subclass for the WhatIsThis menu.
 * Extend this class to implement per-menu behaviors.
 */
public class LazyTickScrollerMenu extends NutKineticMenu.NutItemMenu {
    private int uiPercent = 100;
    private boolean uiForced = false;

    public LazyTickScrollerMenu(Inventory inventory, int containerId, BlockPos pos, ResourceLocation menuId) {
        super(inventory, containerId, pos, menuId);
        syncFromBlockEntity(inventory);
    }

    @Override
    protected void appendAutoSyncVariables(Map<String, Object> variables) {
        super.appendAutoSyncVariables(variables);
        variables.put("menuType", getMenuId().toString());
        variables.put("clt_ui_percent", uiPercent);
        variables.put("clt_ui_forced", uiForced);
    }

    @Override
    protected void handleCustomClientAction(ServerPlayer player, String action, Map<String, Object> variables) {
        if (!"clt_scroller_set".equals(action) || variables == null) {
            return;
        }

        int percent = asInt(variables.get("percent"), uiPercent);
        boolean forced = asBoolean(variables.get("forced"), uiForced);
        applyToBlockEntity(player, percent, forced);
    }

    private void syncFromBlockEntity(Inventory inventory) {
        if (inventory.player == null || inventory.player.level() == null) {
            return;
        }

        BlockEntity be = inventory.player.level().getBlockEntity(getPos());
        if (!(be instanceof ISmartBlockEntityControl control)) {
            return;
        }

        int forcedValue = control.createLazyTick$getForcedValue();
        if (forcedValue != -1) {
            uiForced = true;
            uiPercent = clampPercent(forcedValue);
        } else {
            uiForced = false;
            uiPercent = clampPercent(control.createLazyTick$getDynamicValue());
        }
    }

    private void applyToBlockEntity(ServerPlayer player, int percent, boolean forced) {
        if (player == null || player.level() == null) {
            return;
        }

        BlockEntity be = player.level().getBlockEntity(getPos());
        if (!(be instanceof ISmartBlockEntityControl control)) {
            return;
        }

        int clamped = clampPercent(percent);
        control.createLazyTick$setOwnerName(player.getName().getString());
        control.createLazyTick$setOwnerUUID(player.getUUID());
        LazyTickLogic.switchMode(control, forced, clamped);
        LazyTickLogic.updateState(control);

        uiPercent = clamped;
        uiForced = forced;
    }

    private static int clampPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }



}
