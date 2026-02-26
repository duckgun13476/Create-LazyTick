package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import net.pinkcats.NutUI.menu.architect.Helper.TextureSize;
import org.jetbrains.annotations.NotNull;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

/**
 * Example custom screen for modify-menu ids.
 */
public class LazyTickScrollerScreen extends NutKineticScreen {

    private static final ResourceLocation SCROLLER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "gui/scroller.png");
    private static final double FOLLOW_SMOOTHING = 0.22D;
    private double buttonPosX;
    private double buttonPosY;
    private boolean buttonPositionInitialized;
    private int buttonDrawWidth = 16;
    private int buttonDrawHeight = 16;

    public LazyTickScrollerScreen(NutKineticMenu.NutItemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        TextureSize.Size size = TextureSize.get(SCROLLER_TEXTURE);
        if (size.w() > 0 && size.h() > 0) {
            buttonDrawWidth = size.w();
            buttonDrawHeight = size.h();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Smoothly follow mouse in double precision.
        double targetX = mouseX - this.leftPos - buttonDrawWidth / 2.0D;
        double targetY = mouseY - this.topPos - buttonDrawHeight / 2.0D;

        double minX = 0.0D;
        double minY = 0.0D;
        double maxX = Math.max(0, this.imageWidth - buttonDrawWidth);
        double maxY = Math.max(0, this.imageHeight - buttonDrawHeight);
        targetX = Mth.clamp(targetX, minX, maxX);
        targetY = Mth.clamp(targetY, minY, maxY);

        if (!buttonPositionInitialized) {
            buttonPosX = targetX;
            buttonPosY = targetY;
            buttonPositionInitialized = true;
        } else {
            buttonPosX += (targetX - buttonPosX) * FOLLOW_SMOOTHING;
            buttonPosY += (targetY - buttonPosY) * FOLLOW_SMOOTHING;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw default NutUI background first.
        updateTextureSizeIfNeeded();
        renderDefaultBg(graphics, partialTick, mouseX, mouseY);

        int buttonX = (int) Math.round(buttonPosX);
        int buttonY = (int) Math.round(buttonPosY);

        FancyRender(graphics, buttonX, buttonY, SCROLLER_TEXTURE);

    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }
}
