package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.Lib.mes;
import net.pinkcats.NutUI.menu.Connect.NutUIClientApi;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import net.pinkcats.NutUI.menu.architect.Helper.TextureSize;
import net.pinkcats.NutUI.menu.architect.data.SharedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
    private static final int TRACK_MIN_X = -8;
    private static final int TRACK_MAX_X = 250;
    private static final int TRACK_Y_DYNAMIC = 1;
    private static final int TRACK_Y_FORCED = 12;
    private static final int Y_SWITCH_THRESHOLD = 7;
    private static final int BUTTON_CHAR_COUNT = 4;
    private static final int BUTTON_PIXEL_HEIGHT = 14;
    private double buttonPosX;
    private double buttonPosY;
    private boolean buttonPositionInitialized;
    private int buttonDrawWidth = 16;
    private int buttonDrawHeight = 16;
    private boolean draggingButton = false;

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
        syncFromServerState();

        if (!buttonPositionInitialized) {
            buttonPosX = TRACK_MIN_X;
            buttonPosY = TRACK_Y_DYNAMIC;
            buttonPositionInitialized = true;
        } else if (draggingButton) {
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


        if (buttonPosX < TRACK_MIN_X) buttonPosX = TRACK_MIN_X;
        if (buttonPosX > TRACK_MAX_X) buttonPosX = TRACK_MAX_X;

        int buttonX = (int) Math.round(buttonPosX);
        int buttonY = (int) Math.round(buttonPosY);

        if (buttonY > Y_SWITCH_THRESHOLD) buttonY = TRACK_Y_FORCED;
        if (buttonY <= Y_SWITCH_THRESHOLD) buttonY = TRACK_Y_DYNAMIC;

        mes.warn("{}--{}",buttonPosX,buttonPosY);


        int percent = mapXToPercent(buttonPosX);
        RenderButton(graphics, buttonX, buttonY, percent);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingButton = true;
            applyMouseAndSend((int) Math.round(mouseX), (int) Math.round(mouseY));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingButton && button == 0) {
            applyMouseAndSend((int) Math.round(mouseX), (int) Math.round(mouseY));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingButton = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applyMouseAndSend(int mouseX, int mouseY) {
        double x = toGuiCenteredX(mouseX, buttonDrawWidth);
        double y = toGuiCenteredY(mouseY, buttonDrawHeight);
        if (x < TRACK_MIN_X) x = TRACK_MIN_X;
        if (x > TRACK_MAX_X) x = TRACK_MAX_X;
        int rowY = y > Y_SWITCH_THRESHOLD ? TRACK_Y_FORCED : TRACK_Y_DYNAMIC;

        buttonPosX = x;
        buttonPosY = rowY;

        int percent = mapXToPercent(x);
        boolean forced = rowY == TRACK_Y_FORCED;
        NutUIClientApi.sendAction("clt_scroller_set", Map.of(
                "percent", percent,
                "forced", forced
        ));
    }

    private void syncFromServerState() {
        int percent = SharedData.getSyncedInt("clt_ui_percent", -1);
        if (percent < 0) {
            return;
        }
        if (percent > 100) percent = 100;
        boolean forced = SharedData.getSyncedBoolean("clt_ui_forced", false);

        double targetX = mapPercentToX(percent);
        double targetY = forced ? TRACK_Y_FORCED : TRACK_Y_DYNAMIC;

        if (!draggingButton) {
            buttonPosX = targetX;
            buttonPosY = targetY;
        }
    }

    private static int mapXToPercent(double x) {
        double ratio = (x - TRACK_MIN_X) / (double) (TRACK_MAX_X - TRACK_MIN_X);
        int percent = (int) Math.round(ratio * 100.0D);
        if (percent < 0) return 0;
        if (percent > 100) return 100;
        return percent;
    }

    private static double mapPercentToX(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        double ratio = percent / 100.0D;
        return TRACK_MIN_X + ratio * (TRACK_MAX_X - TRACK_MIN_X);
    }

    private void RenderButton(@NotNull GuiGraphics graphics, int X, int Y, int percent) {
        int drawX = this.leftPos + X;
        int drawY = this.topPos + Y;
        int buttonStartX = drawX;

        //left part
        SBlit(graphics,SCROLLER_BUTTON, drawX, drawY,
                2,2,
                3,14);
        drawX += 3;

        //middle part
        for (int i = 0; i < LazyTickScrollerScreen.BUTTON_CHAR_COUNT; i++) {
            SBlit(graphics, SCROLLER_BUTTON, drawX, drawY,
                    6, 2,
                    5, 14);
            drawX += 5;
        }

        //right part
        SBlit(graphics,SCROLLER_BUTTON, drawX, drawY,
                12,2,
                3,14);

        String percentText = percent + "%";
        int buttonWidth = 3 + (LazyTickScrollerScreen.BUTTON_CHAR_COUNT * 5) + 3;
        int textX = buttonStartX + (buttonWidth - this.font.width(percentText)) / 2 ;
        int textY = drawY + (BUTTON_PIXEL_HEIGHT - 8) / 2;
        graphics.drawString(this.font, percentText, textX+1, textY+1, 0x704630, false);
    }



    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }
}
