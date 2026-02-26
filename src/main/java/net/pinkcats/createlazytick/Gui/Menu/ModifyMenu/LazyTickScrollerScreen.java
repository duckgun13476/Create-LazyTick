package net.pinkcats.createlazytick.Gui.Menu.ModifyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.pinkcats.NutUI.menu.Connect.NutUIClientApi;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.NutKineticScreen;
import net.pinkcats.NutUI.menu.architect.Helper.TextureSize;
import net.pinkcats.NutUI.menu.architect.data.SharedData;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Objects;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

/**
 * Example custom screen for modify-menu ids.
 */
public class LazyTickScrollerScreen extends NutKineticScreen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MODID, "gui/background.png");

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
    private static final int DEFAULT_PERCENT_FALLBACK = 50;
    private static final int BACKGROUND_TILE = 4;
    private static final int BACKGROUND_ROWS = 15;
    private static final int BACKGROUND_SPREAD_MAX = 34;
    private double buttonPosX;
    private double buttonPosY;
    private boolean buttonPositionInitialized;
    private int buttonDrawWidth = 16;
    private int buttonDrawHeight = 16;
    private boolean draggingButton = false;
    private boolean rightMouseDownLastFrame = false;

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
        boolean rightMouseDown = isRightMouseDown();
        if (rightMouseDownLastFrame && !rightMouseDown) {
            onClose();
            return;
        }
        rightMouseDownLastFrame = rightMouseDown;
        draggingButton = rightMouseDown;
        if (!buttonPositionInitialized) {
            initializeFromServerState();
            buttonPositionInitialized = true;
        } else if (draggingButton) {
            buttonPosX = smoothFollowCenteredX(buttonPosX, mouseX, buttonDrawWidth, FOLLOW_SMOOTHING);
            buttonPosY = smoothFollowCenteredY(buttonPosY, mouseY, buttonDrawHeight, FOLLOW_SMOOTHING);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

        //Init
        Component topHint = Component.translatable("createlazytick.scroller.target_lazytick_config");
        Component bottomHint = Component.translatable("createlazytick.scroller.release_right_click_confirm");
        int textCenterX = this.leftPos + (TRACK_MIN_X + TRACK_MAX_X) / 2;
        int topHintX = textCenterX - this.font.width(topHint) / 2;
        int bottomHintX = textCenterX - this.font.width(bottomHint) / 2;
        updateTextureSizeIfNeeded();

        // Draw default NutUI background first.
        RenderBackground(graphics,textCenterX,this.topPos - 20);
        renderDefaultBg(graphics, partialTick, mouseX, mouseY);

        if (buttonPosX < TRACK_MIN_X) buttonPosX = TRACK_MIN_X;
        if (buttonPosX > TRACK_MAX_X) buttonPosX = TRACK_MAX_X;

        int buttonX = (int) Math.round(buttonPosX);
        int buttonY = (int) Math.round(buttonPosY);

        if (buttonY > Y_SWITCH_THRESHOLD) buttonY = TRACK_Y_FORCED;
        if (buttonY <= Y_SWITCH_THRESHOLD) buttonY = TRACK_Y_DYNAMIC;


        int percent = mapXToPercent(buttonPosX);

        RenderButton(graphics, buttonX, buttonY, percent);
        graphics.drawString(this.font, topHint, topHintX+6, this.topPos - 10, 0xFFFFFF, false);
        graphics.drawString(this.font, bottomHint, bottomHintX+5, this.topPos + 28, 0xFFFFFF, false);

    }

    private int BackGroundTick = 0;

    private void RenderBackground(@NotNull GuiGraphics graphics,int X,int Y) {
        if (BackGroundTick < BACKGROUND_SPREAD_MAX) {
            BackGroundTick ++;
        }

        drawBackgroundColumn(graphics, X, Y);
        for (int i = 1; i <= BackGroundTick; i++) {
            int offset = i * BACKGROUND_TILE;
            drawBackgroundColumn(graphics, X - offset, Y);
            drawBackgroundColumn(graphics, X + offset, Y);
        }
    }

    private void drawBackgroundColumn(@NotNull GuiGraphics graphics, int x, int y) {
        int drawY = y;
        for (int i = 0; i < BACKGROUND_ROWS; i++) {
            SBlit(graphics, BACKGROUND, x, drawY,
                    1, 1,
                    BACKGROUND_TILE, BACKGROUND_TILE);
            drawY += BACKGROUND_TILE;
        }
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


    private boolean isSyncedStateForCurrentMenu() {
        String syncedMenuId = SharedData.getSyncedString("menuId", "");
        int[] syncedPos = SharedData.getSyncedObject("pos", int[].class);
        if (syncedPos == null || syncedPos.length != 3) {
            return false;
        }

        String currentMenuId = out_menu.getMenuId().toString();
        if (!Objects.equals(currentMenuId, syncedMenuId)) {
            return false;
        }

        return syncedPos[0] == out_menu.getPos().getX()
                && syncedPos[1] == out_menu.getPos().getY()
                && syncedPos[2] == out_menu.getPos().getZ();
    }

    private void initializeFromServerState() {
        if (!isSyncedStateForCurrentMenu()) {
            buttonPosX = mapPercentToX(DEFAULT_PERCENT_FALLBACK);
            buttonPosY = TRACK_Y_DYNAMIC;
            return;
        }

        int percent = SharedData.getSyncedInt("clt_ui_percent", -1);
        if (percent >= 0) {
            buttonPosX = mapPercentToX(percent);
            boolean forced = SharedData.getSyncedBoolean("clt_ui_forced", false);
            buttonPosY = forced ? TRACK_Y_FORCED : TRACK_Y_DYNAMIC;
            return;
        }

        buttonPosX = mapPercentToX(DEFAULT_PERCENT_FALLBACK);
        buttonPosY = TRACK_Y_DYNAMIC;
    }

    //Tool func
    private static boolean isRightMouseDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static int mapXToPercent(double x) {
        double ratio = (x - TRACK_MIN_X) / (double) (TRACK_MAX_X - TRACK_MIN_X);
        int percent = (int) Math.round(ratio * 100.0D);
        if (percent < 0) return 0;
        return Math.min(percent, 100);
    }

    private static double mapPercentToX(int percent) {
        int clamped = percent;
        if (clamped < 0) clamped = 0;
        if (clamped > 100) clamped = 100;
        double ratio = clamped / 100.0D;
        return TRACK_MIN_X + ratio * (TRACK_MAX_X - TRACK_MIN_X);
    }

    private void sendCurrentStateToServer() {
        if (!buttonPositionInitialized) {
            initializeFromServerState();
            buttonPositionInitialized = true;
        }
        double x = buttonPosX;
        if (x < TRACK_MIN_X) x = TRACK_MIN_X;
        if (x > TRACK_MAX_X) x = TRACK_MAX_X;
        int rowY = buttonPosY > Y_SWITCH_THRESHOLD ? TRACK_Y_FORCED : TRACK_Y_DYNAMIC;
        int percent = mapXToPercent(x);
        boolean forced = rowY == TRACK_Y_FORCED;
        NutUIClientApi.sendAction("clt_scroller_set", Map.of(
                "percent", percent,
                "forced", forced
        ));
    }

    @Override
    public void onClose() {
        sendCurrentStateToServer();
        super.onClose();
    }


    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
