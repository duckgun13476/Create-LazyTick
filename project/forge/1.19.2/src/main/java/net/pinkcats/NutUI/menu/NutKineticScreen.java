package net.pinkcats.NutUI.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
    protected void renderBg(@NotNull PoseStack poseStack, float pPartialTick, int pMouseX, int pMouseY) {
        //Sync size
        updateTextureSizeIfNeeded();

        renderDefaultBg(poseStack, pPartialTick, pMouseX, pMouseY);
    }

    /**
     * Default background pipeline.
     * Subclasses can call this and then add custom rendering.
     */
    protected void renderDefaultBg(@NotNull PoseStack poseStack, float pPartialTick, int pMouseX, int pMouseY) {

        //Define pos
        int MenuStartPosX = this.leftPos;
        int MenuStartPosY = this.topPos;

        //Show Box
        FancyRender(poseStack, MenuStartPosX, MenuStartPosY);

        extraAreas = List.of(tempAreas.toArray(new Rect2i[0]));
        tempAreas.clear();
    }



    /**
     * Title Show Func
     */
    @Override
    protected void renderLabels(@NotNull PoseStack poseStack, int pMouseX, int pMouseY) {
        //Show Name
        MutableComponent headerTitle = Component.translatable("menu.transfer_block_container");
        MutableComponent requireTitle = Component.translatable("menu.transfer_block_require");
        this.font.draw(poseStack, headerTitle, this.titleLabelX + 50, this.titleLabelY - 38, 0x714A40);
        this.font.draw(poseStack, requireTitle, this.inventoryLabelX - 56, this.inventoryLabelY + 32, 0x714A40);


//        //Show parameter
//        String syncState = SharedData.getSyncedBoolean(DataPacket.DEMO_SYNC_KEY, false) ? "OK" : "WAIT";
//        long syncTick = SharedData.getSyncedInt(DataPacket.DEMO_SYNC_TICK_KEY, -1);
//        String demoLine = "SyncDemo [" + syncState + "] tick=" + syncTick;
//        this.font.draw(poseStack, demoLine, this.inventoryLabelX - 56, this.inventoryLabelY + 44, 0x3F6E5E);

    }


    /// ///


    /**
     * Tool Func
     */
    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(poseStack, mouseX, mouseY);
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
        // mes.info("[TextureSize] tex=" + tex + " size=" + s.w() + "x" + s.h());

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
    protected void FancyRender(PoseStack poseStack, int MSX, int MSY) {
        NutMenuInfo.data info = NutMenuInfo.require(out_menu.getMenuId());
        fancyRenderAbsolute(
                poseStack,
                MSX, MSY,
                info.texture(),
                info.textureStartX(), info.textureStartY(),
                this.ScreenWidth, this.ScreenHeight,
                this.TextureWidth, this.TextureHeight
        );
    }

    /**
     *  Fancy Render Add new UI
     */
    protected void FancyRender(PoseStack poseStack, int MSX, int MSY, ResourceLocation texture) {
        // Default aligns with parent start point, convenient for later position tuning.
        NutMenuInfo.data info = NutMenuInfo.require(out_menu.getMenuId());
        FancyRender(poseStack, MSX, MSY, texture, info.textureStartX(), info.textureStartY());
    }

    protected void FancyRender(PoseStack poseStack, int MSX, int MSY, ResourceLocation texture, int u, int v) {
        TextureSize.Size size = TextureSize.get(texture);
        int texWidth = size.w() > 0 ? size.w() : this.ScreenWidth;
        int texHeight = size.h() > 0 ? size.h() : this.ScreenHeight;

        // Standalone layer mode: draw at texture's own size (1:1) by default.
        FancyRender(poseStack, MSX, MSY, texture, u, v, texWidth, texHeight, texWidth, texHeight);
    }

    protected void FancyRender(PoseStack poseStack, int MSX, int MSY, ResourceLocation texture,
                               int u, int v, int drawWidth, int drawHeight, int textureWidth, int textureHeight) {
        // Custom-layer coordinates are relative to the parent UI top-left.
        int drawX = this.leftPos + MSX;
        int drawY = this.topPos + MSY;
        fancyRenderAbsolute(poseStack, drawX, drawY, texture, u, v, drawWidth, drawHeight, textureWidth, textureHeight);
    }

    private void fancyRenderAbsolute(PoseStack poseStack, int drawX, int drawY, ResourceLocation texture,
                                     int u, int v, int drawWidth, int drawHeight, int textureWidth, int textureHeight) {
        //mes.warn("{}{}{}{}{}{}{}",texture,drawX, drawY, 0, 0, this.imageWidth,this.imageHeight);
        bindTexture(texture);
        blit(poseStack, drawX, drawY, u, v, drawWidth, drawHeight, textureWidth, textureHeight);
        //PlaceStrip JEI block
        tempAreas.add(new Rect2i(drawX + 5, drawY + 5, EnsurePositive(drawWidth - 10), EnsurePositive(drawHeight - 10)));
    }

    //*
    // Replace Vanilla interface
    // */
    protected void SBlit(
            @NotNull PoseStack poseStack, ResourceLocation atlasLocation,
            int x, int y,
            float UV_StartPoint_X, float UV_StartPoint_Y,
            int UV_Width, int UV_Height){
        TextureSize.Size size = TextureSize.get(atlasLocation);
        int texWidth = size.w();
        int texHeight = size.h();
        bindTexture(atlasLocation);
        blit(poseStack,
                x, y,
                UV_StartPoint_X-1, UV_StartPoint_Y-1, // Texture startpoint
                UV_Width, UV_Height,
                texWidth, texHeight);
    }

    private void bindTexture(ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
    }

    @Deprecated
    protected void debugWindowArea(PoseStack poseStack) {
        fill(poseStack, leftPos + imageWidth, topPos + imageHeight, leftPos, topPos, 0xD3D3D3D3);
    }

    @Deprecated
    protected void debugExtraAreas(PoseStack poseStack) {
        mes.warn("debugExtraAreas");
        for (Rect2i area : getExtraAreas()) {
            fill(poseStack, area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(),
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

    // Reusable helpers for cursor-following UI elements
    protected double toGuiCenteredX(int mouseX, int drawWidth) {
        return mouseX - this.leftPos - drawWidth / 2.0D;
    }

    protected double toGuiCenteredY(int mouseY, int drawHeight) {
        return mouseY - this.topPos - drawHeight / 2.0D;
    }

    protected double smoothApproach(double current, double target, double smoothing) {
        return current + (target - current) * smoothing;
    }

    // Smooth follow only, no clamping.
    protected double smoothFollowCenteredX(double current, int mouseX, int drawWidth, double smoothing) {
        return smoothApproach(current, toGuiCenteredX(mouseX, drawWidth), smoothing);
    }

    // Smooth follow only, no clamping.
    protected double smoothFollowCenteredY(double current, int mouseY, int drawHeight, double smoothing) {
        return smoothApproach(current, toGuiCenteredY(mouseY, drawHeight), smoothing);
    }


}

