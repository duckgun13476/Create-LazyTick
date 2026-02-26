package net.pinkcats.NutUI.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.Lib.mes;
import net.pinkcats.NutUI.menu.architect.Helper.TextureSize;
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;
import net.pinkcats.NutUI.menu.architect.data.SharedData;
import net.pinkcats.NutUI.menu.Connect.NutUIClientApi;

import net.pinkcats.NutUI.menu.Connect.DataPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NutKineticScreen extends AbstractContainerScreen<NutKineticMenu.NutItemMenu> {

    private final List<Rect2i> tempAreas = new ArrayList<>();
    private List<Rect2i> extraAreas = Collections.emptyList();


    //private final TransferBoxEntity transferEntity;
    protected final NutKineticMenu.NutItemMenu out_menu; // 娣诲姞鎴愬憳鍙橀噺

    private int ScreenWidth;
    private int ScreenHeight;
    private int TextureWidth;
    private int TextureHeight;
    private ResourceLocation lastTexture;

    public NutKineticScreen(NutKineticMenu.NutItemMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.inventoryLabelY = this.imageHeight - 110;
       // this.transferEntity = this.menu.getTransferEntity();
        this.out_menu = pMenu;
    }



    @Override
    public void init() {
        super.init();
        updateTextureSizeIfNeeded();
    }



    /**
     * Screen's Render Entrance
     */
    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        //Sync size
        updateTextureSizeIfNeeded();

        renderDefaultBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
    }

    /**
     * Default background pipeline.
     * Subclasses can call this and then add custom rendering.
     */
    protected void renderDefaultBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {

        //Define pos
        int MenuStartPosX = this.leftPos;
        int MenuStartPosY = this.topPos;

        //Show Box
        FancyRender(pGuiGraphics, MenuStartPosX, MenuStartPosY);

        extraAreas = List.of(tempAreas.toArray(new Rect2i[0]));
        tempAreas.clear();
    }



    /**
     * Title Show Func
     */
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        //Show Name
        MutableComponent headerTitle = Component.translatable("menu.transfer_block_container");
        MutableComponent requireTitle = Component.translatable("menu.transfer_block_require");
        pGuiGraphics.drawString(this.font, headerTitle, this.titleLabelX+50, this.titleLabelY-38, 0x714A40, false);
        pGuiGraphics.drawString(this.font, requireTitle,  this.inventoryLabelX-56, this.inventoryLabelY+32, 0x714A40, false);


        //Show parameter
        String syncState = SharedData.getSyncedBoolean(DataPacket.DEMO_SYNC_KEY, false) ? "OK" : "WAIT";
        long syncTick = SharedData.getSyncedInt(DataPacket.DEMO_SYNC_TICK_KEY, -1);
        String demoLine = "SyncDemo [" + syncState + "] tick=" + syncTick;
        pGuiGraphics.drawString(this.font, demoLine, this.inventoryLabelX-56, this.inventoryLabelY+44, 0x3F6E5E, false);

    }


    /// ///


    /**
     * Tool Func
     */
    @Override
    public void render(@NotNull GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    /**
     * Tool Func
     */
    private int EnsurePositive (int Length){
        return Math.max(Length, 1);
    }

    protected void updateTextureSizeIfNeeded() {
        if (out_menu == null) {
            return;
        }

        NutMenuInfo.data info = NutMenuInfo.require(out_menu.getMenuId());
        ResourceLocation tex = info.texture();
        if (tex.equals(this.lastTexture)) {
            return;
        }

        TextureSize.Size s = TextureSize.get(tex);
        mes.info("[TextureSize] tex=" + tex + " size=" + s.w() + "x" + s.h());

        this.lastTexture = tex;
        this.TextureWidth = s.w();
        this.TextureHeight = s.h();
        this.ScreenWidth = (info.w() == null || info.w() <= 0) ? this.TextureWidth : info.w();
        this.ScreenHeight = (info.h() == null || info.h() <= 0) ? this.TextureHeight : info.h();
        this.imageWidth = this.ScreenWidth;
        this.imageHeight = this.ScreenHeight;
        this.inventoryLabelY = this.imageHeight - 110;
        this.leftPos = (this.width - this.imageWidth) / 2 + info.x();
        this.topPos = (this.height - this.imageHeight) / 2 + info.y();
    }



    /**
     * Copy from Create for JEI hide problem
     */

    public List<Rect2i> getExtraAreas() {
        return extraAreas;//Collections.emptyList();
    }


    /**
     *  Fancy Render for add Menu With box
     */
    protected void FancyRender(GuiGraphics pGuiGraphics, int MSX, int MSY) {
        int I_WIDTH = this.ScreenWidth;
        int I_HEIGHT = this.ScreenHeight;

        NutMenuInfo.data info = NutMenuInfo.require(out_menu.getMenuId());
        ResourceLocation texture = info.texture();
        mes.warn("{}{}{}{}{}{}{}",texture,MSX, MSY, 0, 0, this.imageWidth,this.imageHeight);
        pGuiGraphics.blit(texture, MSX, MSY, info.textureStartX(), info.textureStartY(),
                I_WIDTH, I_HEIGHT, this.TextureWidth, this.TextureHeight);
        //PlaceStrip JEI block
        tempAreas.add(new Rect2i(MSX+5, MSY+5, EnsurePositive(I_WIDTH-10), EnsurePositive(I_HEIGHT-10)));
    }

    @Deprecated
    protected void debugWindowArea(GuiGraphics graphics) {
        graphics.fill(leftPos + imageWidth, topPos + imageHeight, leftPos, topPos, 0xD3D3D3D3);
    }

    @Deprecated
    protected void debugExtraAreas(GuiGraphics graphics) {
        mes.warn("debugExtraAreas");
        for (Rect2i area : getExtraAreas()) {
            graphics.fill(area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(),
                    0xD3D3D3D3);
        }
    }
    protected void playUiSound(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(sound, pitch, volume * 0.25f));
    }

    protected void setServerKey(String key, Object value) {
        NutUIClientApi.sendAction("set_key", key, value);
    }


}

