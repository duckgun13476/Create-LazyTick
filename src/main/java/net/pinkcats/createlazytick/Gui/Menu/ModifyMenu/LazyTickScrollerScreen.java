package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.Lib.mes;
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

    private static final ResourceLocation SCROLLER_BUTTON =
            ResourceLocation.fromNamespaceAndPath(MODID, "gui/button.png");

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
        TextureSize.Size size = TextureSize.get(SCROLLER_BUTTON);
        if (size.w() > 0 && size.h() > 0) {
            buttonDrawWidth = size.w();
            buttonDrawHeight = size.h();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!buttonPositionInitialized) {
            buttonPosX = toGuiCenteredX(mouseX, buttonDrawWidth);
            buttonPosY = toGuiCenteredY(mouseY, buttonDrawHeight);
            buttonPositionInitialized = true;
        } else {
            buttonPosX = smoothFollowCenteredX(buttonPosX, mouseX, buttonDrawWidth, FOLLOW_SMOOTHING);
            buttonPosY = smoothFollowCenteredY(buttonPosY, mouseY, buttonDrawHeight, FOLLOW_SMOOTHING);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw default NutUI background first.
        updateTextureSizeIfNeeded();
        renderDefaultBg(graphics, partialTick, mouseX, mouseY);


        if (buttonPosX<-8) buttonPosX=-8;
        if (buttonPosX>250) buttonPosX=250;

        int buttonX = (int) Math.round(buttonPosX);
        int buttonY = (int) Math.round(buttonPosY);

        if (buttonY>7) buttonY=12;
        if (buttonY<=7) buttonY=1;

        mes.warn("{}--{}",buttonPosX,buttonPosY);


        RenderButton(graphics, buttonX, buttonY, 3);

    }

    private void RenderButton(@NotNull GuiGraphics graphics,int X,int Y,int Char) {
        int drawX = this.leftPos + X;
        int drawY = this.topPos + Y;

        //left part
        SBlit(graphics,SCROLLER_BUTTON, drawX, drawY,
                2,2,
                3,14);
        drawX += 3;

        //middle part
        for (int i = 0; i < Char; i++) {
            SBlit(graphics, SCROLLER_BUTTON, drawX, drawY,
                    6, 2,
                    5, 14);
            drawX += 5;
        }

        //right part
        SBlit(graphics,SCROLLER_BUTTON, drawX, drawY,
                12,2,
                3,14);
    }



    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }
}
