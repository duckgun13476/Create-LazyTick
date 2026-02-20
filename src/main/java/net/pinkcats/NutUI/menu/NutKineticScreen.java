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
import net.pinkcats.NutUI.menu.architect.data.CoordinateData;
import net.pinkcats.NutUI.menu.architect.data.SharedData;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.NutUI.menu.Connect.Channel;
import net.pinkcats.NutUI.menu.Connect.DataPacket;
import net.pinkcats.NutUI.menu.architect.NutItemMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.pinkcats.NutUI.menu.architect.ResourceParse.LoadTextureN;


public class NutKineticScreen extends AbstractContainerScreen<NutItemMenu> {


    private List<Rect2i> tempAreas = new ArrayList<>();
    // 2. 最终存储不可变列表的变量（初始为空）
    private List<Rect2i> extraAreas = Collections.emptyList();


    // resources/assets/modid/gui/transfer_menu.png
    public static final ResourceLocation TransferBoxGUI = LoadTextureN("gui/transfer_menu.png");


    private EditBox textF;
    private EditBox textY;
    private EditBox textZ;
    //private final TransferBoxEntity transferEntity;
    private final NutItemMenu out_menu; // 添加成员变量
    public NutKineticScreen(NutItemMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
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

    private void sendY() {
        CompoundTag mainTag = new CompoundTag();
        mainTag.putInt("count", menu.count);
        mainTag.putBoolean("lessThan", menu.lessThan);
        //   System.out.println(menu.count);
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
                        coordinateData.setDimensionsY(menu.count);
                        modifiedCoordinate = coordinateData; // 记录修改的元素


                    }
                }
            }

            if (modifiedCoordinate != null) {
                coordinatesList.remove(modifiedCoordinate); // 移除修改的元素
                coordinatesList.add(modifiedCoordinate); // 添加到列表末尾
            }

            SharedData.setCoordinatesList(coordinatesList);
            Channel.sendToServer(new DataPacket(SharedData.getDimension(),SharedData.getCoordinatesList()));

       // }
    }
    //Z
    private void sendZ() {
        CompoundTag mainTag = new CompoundTag();
        mainTag.putInt("count", menu.count);
        mainTag.putBoolean("lessThan", menu.lessThan);

       // if (transferEntity != null) {
            SharedData.setDimension(menu.count);

            //Change
            List<CoordinateData> coordinatesList = SharedData.getCoordinatesList();
            CoordinateData modifiedCoordinate = null; // 用于存储修改的元素

            for (CoordinateData coordinateData : coordinatesList) {
                if (coordinateData != null) {
                    if (Arrays.equals(
                            coordinateData.getPos(),
                            new int[]{out_menu.getPos().getX(), out_menu.getPos().getY(), out_menu.getPos().getZ()})) {

                        // 修改数值
                        coordinateData.setDimensionsZ(menu.count);
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
            Channel.sendToServer(new DataPacket(SharedData.getDimension(),SharedData.getCoordinatesList()));

       // }
    }
    @Override
    public void init() {
        super.init();
        if(out_menu==null)
            return;


        // this.font = Minecraft.getInstance().font;
        //text X
        //textF = new EditBox(font, leftPos + 84, topPos + 68-18, 21, font.lineHeight, Component.translatable("narrator.transfer.dimensionX"));
        //textF.setMaxLength(3);
        //textF.setBordered(false);
        //textF.setVisible(true);
        // textF.setTextColor(0xFFFFFF);
        // textF.setValue("维度 dimension"
        //   );
        //  textF.setResponder(t -> {
        //     try {
        //         int c = Integer.parseInt(t);
        //        textF.setTextColor(0x00FF00);
        //        if(c >= 0) {
        //             menu.count = c;
        //               sendX();
        //         }
        //    } catch (NumberFormatException e) {
        //        textF.setTextColor(0xFF0000);
        //      }
        // });
        //    addRenderableWidget(textF);
        //text Y
        //  textY = new EditBox(font, leftPos + 114, topPos + 68-18, 21, font.lineHeight, Component.translatable("narrator.transfer.dimensionX"));
        //  textY.setMaxLength(3);
        //  textY.setBordered(false);
        //   textY.setVisible(true);
        //  textY.setTextColor(0xFFFFFF);
        //    textY.setValue("维度 dimension"
        //  );
        //   textY.setResponder(t -> {
        //  try {
        //       int c = Integer.parseInt(t);
        //       textY.setTextColor(0x00FF00);
        //       if(c >= 0) {
        //             menu.count = c;
        //             sendY();
        //        }
        //     } catch (NumberFormatException e) {
        //        textY.setTextColor(0xFF0000);
        //      }
        //  });
        //    addRenderableWidget(textY);
        // //text Z
        //  textZ = new EditBox(font, leftPos + 144, topPos + 68-18, 21, font.lineHeight, Component.translatable("narrator.transfer.dimensionX"));
        //   textZ.setMaxLength(3);
        //  textZ.setBordered(false);
        // textZ.setVisible(true);
        //  textZ.setTextColor(0xFFFFFF);
        //  textZ.setValue("维度 dimension"
        //   );
        //  textZ.setResponder(t -> {
        //     try {
        //        int c = Integer.parseInt(t);
        //      textZ.setTextColor(0x00FF00);
        //      if(c >= 0) {
        //       menu.count = c;
        //          sendZ();
        //       }
        //    } catch (NumberFormatException e) {
        //         textZ.setTextColor(0xFF0000);
        //      }
        //   });
        //   addRenderableWidget(textZ);

        // addRenderableWidget(
        //        new StateChooseButton(leftPos+136,topPos+12-18,28,15,out_menu.getPos(),Component.empty(), button -> {}));

        // addRenderableWidget(
        //        new StatusButton(leftPos+11,topPos+12-18,16,16,out_menu.getPos(),Component.empty(), button -> {}));


        // addRenderableWidget(
        //  new SwitchButton(leftPos+117,topPos+12-18,16,16,out_menu.getPos(),Component.empty(), button -> {}));



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

        // Info   ：        width 245
        // Search 0-35
        // SearchAttach 42-61
        // RequestBox   72-105
        // NoRequestEnd  112-125
        // InventoryStart 132-149
        // InvMiddle     155-174
        // InvTail   181-199

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




        // pGuiGraphics.drawCenteredString(
        //         Minecraft.getInstance().font,
        //           Component.literal("α      β      θ"),
        //            this.leftPos+123,this.topPos-18+55,0x666666
        //  );


        // pGuiGraphics.drawCenteredString(
        //          Minecraft.getInstance().font,
        //      Component.literal("美味的紫月"),
        //      25,  15, 0xffffff);
        // pGuiGraphics.drawCenteredString(
        //    Minecraft.getInstance().font,
        //    Component.literal("美味的🐟🐟"),
        //    25,  35, 0xffffff);



        //pGuiGraphics.blit(EASYSCREENPATH,50,50,0,0,256,256);

        //super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);

        extraAreas = List.of(tempAreas.toArray(new Rect2i[0]));
        tempAreas.clear();
    }

    /**
     *  Fancy Render for add Menu With box
     */
    protected void FancyRender(GuiGraphics pGuiGraphics, int MSX, int MSY,int ISX, int ISY,int I_WIDTH, int I_HEIGHT) {
        pGuiGraphics.blit(TransferBoxGUI, MSX, MSY, ISX, ISY, I_WIDTH, I_HEIGHT);
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

        //Show Name
        pGuiGraphics.drawString(this.font, headerTitle, this.titleLabelX+50, this.titleLabelY-38, 0x714A40, false);

        pGuiGraphics.drawString(this.font, requireTitle,  this.inventoryLabelX-56, this.inventoryLabelY+32, 0x714A40, false);



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
