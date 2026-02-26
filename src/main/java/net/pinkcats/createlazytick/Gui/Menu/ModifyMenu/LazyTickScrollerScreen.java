package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.Lib.mes;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import org.jetbrains.annotations.NotNull;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

/**
 * Example custom screen for modify-menu ids.
 */
public class LazyTickScrollerScreen extends NutKineticScreen {

    private static final ResourceLocation SCROLLER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "gui/scroller.png");

    public LazyTickScrollerScreen(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw default NutUI background first.
        updateTextureSizeIfNeeded();

        renderDefaultBg(graphics, partialTick, mouseX, mouseY);
        // Render button
        FancyRender(graphics, 0, 0, SCROLLER_TEXTURE);

    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }
}
