package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.Lib.mes;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import org.jetbrains.annotations.NotNull;

/**
 * Example custom screen for modify-menu ids.
 */
public class LazyTickScrollerScreen extends NutKineticScreen {

    public LazyTickScrollerScreen(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        //Sync size
        updateTextureSizeIfNeeded();

        renderDefaultBg(graphics, partialTick, mouseX, mouseY);
        mes.warn("modify bg");

    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }
}
