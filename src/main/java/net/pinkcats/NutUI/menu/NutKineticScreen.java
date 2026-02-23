package net.pinkcats.NutUI.menu;



import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.pinkcats.NutUI.menu.architect.Helper.TextureSize;
import net.pinkcats.NutUI.menu.architect.data.CoordinateData;
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;
import net.pinkcats.NutUI.menu.architect.data.SharedData;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.NutUI.menu.Connect.Channel;
import net.pinkcats.NutUI.menu.Connect.DataPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class NutKineticScreen extends AbstractContainerScreen<NutKineticMenu.NutItemMenu> {

    private static int DEMO_CLIENT_OPEN_COUNTER = 0;

    private List<Rect2i> tempAreas = new ArrayList<>();
    // 2. 最终存储不可变列表的变量（初始为空）
    private List<Rect2i> extraAreas = Collections.emptyList();



    private EditBox textF;
    private EditBox textY;
    private EditBox textZ;
    //private final TransferBoxEntity transferEntity;
    private final NutKineticMenu.NutItemMenu out_menu; // 添加成员变量
    public NutKineticScreen(NutKineticMenu.NutItemMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.inventoryLabelY = this.imageHeight - 110;
       // this.transferEntity = this.menu.getTransferEntity();
        this.out_menu = pMenu;
    }



    private void sendX() {
        CompoundTag mainTag = new CompoundTag();
        mainTag.putInt("count", menu.count);
        mainTag.putBoolean("lessThan", menu.lessThan);

        //if (transferEntity != null) {

            SharedData.setDimension(menu.count);
            List<CoordinateData> coordinatesList = SharedData.getCoordinatesList();
            CoordinateData modifiedCoordinate = null; // 用于存储修改的元素

            for (CoordinateData coordinateData : coordinatesList) {
                if (coordinateData != null) {
                    if (Arrays.equals(
                            coordinateData.getPos(),
                            new int[]{out_menu.getPos().getX(), out_menu.getPos().getY(), out_menu.getPos().getZ()})) {

                        // 修改数值
                        coordinateData.setDimensionsX(menu.count);
                        modifiedCoordinate = coordinateData; // 记录修改的元素


                    }
                }
            }

            // 如果有元素被修改，则将其移动到列表末尾
            if (modifiedCoordinate != null) {
                coordinatesList.remove(modifiedCoordinate); // 移除修改的元素
                coordinatesList.add(modifiedCoordinate); // 添加到列表末尾
            }

            // 更新共享数据
            SharedData.setCoordinatesList(coordinatesList);
            Channel.sendToServer(new DataPacket(SharedData.getDimension(), SharedData.getCoordinatesList()));

        //}
        ///Channel.sendToPlayer(new DataPacket(menu.count,DARA,(ServerPlayer) event.getEntity));
    }

    @Override
    public void init() {
        super.init();
        if(out_menu==null)
            return;

        ResourceLocation tex = NutMenuInfo.require(out_menu.getMenuId()).texture();
        TextureSize.Size s = TextureSize.get(tex);
        mes.info("[TextureSize] tex=" + tex + " size=" + s.w() + "x" + s.h());
        DEMO_CLIENT_OPEN_COUNTER++;
        Channel.sendToServer(new DataPacket(
                SharedData.getDimension(),
                SharedData.getCoordinatesList(),
                Map.of(
                        DataPacket.DEMO_CLIENT_OPEN_COUNTER_KEY, DEMO_CLIENT_OPEN_COUNTER,
                        "menuId", out_menu.getMenuId().toString()
                )
        ));


    }



    /**
     * Screen's Render Entrance
     */
    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        //Prevent leak

        int ImageWidth = 245;    //ImageWidth
        int StartPointX = 0;    //ImageStartPoint X

        int MenuStartPosX = this.leftPos-35;

        int MenuStartPosY = this.topPos-36;

        int MenuChangePosY = MenuStartPosY;

        //Show base picture
        //pGuiGraphics.blit(TransferBoxGUI,this.leftPos,this.topPos-18,0,0,this.imageWidth,this.imageHeight);

        //Show Box
        int BoxLine = 4;
        FancyRender(pGuiGraphics,
                MenuStartPosX, MenuChangePosY,                         //ShowPointOnScreen X Y
                StartPointX, 0,                           //StartPoint X Y
                ImageWidth, 35);                            //ImageSize W L
        MenuChangePosY += 35;
        for (int i=0;i<BoxLine;i++) {
            FancyRender(pGuiGraphics,
                    MenuStartPosX, MenuChangePosY+(19*i),
                    StartPointX, 42,
                    ImageWidth, 19);}
        MenuChangePosY += 19 * (BoxLine);
        FancyRender(pGuiGraphics,
                MenuStartPosX, MenuChangePosY,
                StartPointX, 72,
                ImageWidth, 33);

        //Show Tip libel
        // 164 206
        // 230 228
        FancyRender(pGuiGraphics,
                MenuStartPosX-36, MenuChangePosY+3,
                164, 206,
                67, 23);



        MenuChangePosY += 35;
        //Show Player Inventory
        int InventoryLine = 4;
        FancyRender(pGuiGraphics,
                MenuStartPosX, MenuChangePosY,
                StartPointX, 132,
                ImageWidth, 17);
        for (int i=0;i<InventoryLine-1;i++) {
            FancyRender(pGuiGraphics,
                    MenuStartPosX, MenuChangePosY+17+(19*i),
                    StartPointX, 155,
                    ImageWidth, 19);}

        FancyRender(pGuiGraphics,
                MenuStartPosX, MenuChangePosY+17+19*(InventoryLine-1),
                StartPointX, 181,
                ImageWidth, 19);


        extraAreas = List.of(tempAreas.toArray(new Rect2i[0]));
        tempAreas.clear();
    }

    /**
     *  Fancy Render for add Menu With box
     */
    protected void FancyRender(GuiGraphics pGuiGraphics, int MSX, int MSY,int ISX, int ISY,int I_WIDTH, int I_HEIGHT) {
        ResourceLocation tex = NutMenuInfo.require(out_menu.getMenuId()).texture();

        pGuiGraphics.blit(tex, MSX, MSY, ISX, ISY, I_WIDTH, I_HEIGHT);
        //PlaceStrip JEI block
        tempAreas.add(new Rect2i(MSX+5, MSY+5, EnsurePositive(I_WIDTH-10), EnsurePositive(I_HEIGHT-10)));
    }

    /**
     * Title Show Func
     */
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        MutableComponent headerTitle = Component.translatable("menu.transfer_block_container");
        MutableComponent requireTitle = Component.translatable("menu.transfer_block_require");
        int serverCounter = SharedData.getSyncedInt(DataPacket.DEMO_SERVER_COUNTER_KEY, -1);
        String syncState = SharedData.getSyncedBoolean(DataPacket.DEMO_SYNC_KEY, false) ? "OK" : "WAIT";
        String demoLine = "SyncDemo [" + syncState + "] client=" + DEMO_CLIENT_OPEN_COUNTER + " server=" + serverCounter;

        //Show Name
        pGuiGraphics.drawString(this.font, headerTitle, this.titleLabelX+50, this.titleLabelY-38, 0x714A40, false);

        pGuiGraphics.drawString(this.font, requireTitle,  this.inventoryLabelX-56, this.inventoryLabelY+32, 0x714A40, false);
        pGuiGraphics.drawString(this.font, demoLine, this.inventoryLabelX-56, this.inventoryLabelY+44, 0x3F6E5E, false);


    }



    /**
     * Tool Func
     */
    @Override
    public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {

        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    /**
     * Tool Func
     */
    private int EnsurePositive (int Length){
        return Math.max(Length, 1);
    }



    /**
     * Copy from Create for JEI hide problem
     */

    public List<Rect2i> getExtraAreas() {

        return extraAreas;//Collections.emptyList();
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



}
