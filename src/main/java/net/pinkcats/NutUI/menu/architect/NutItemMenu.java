package net.pinkcats.NutUI.menu.architect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.pinkcats.NutUI.menu.MenuLib;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.NutUI.menu.architect.slots._SlotBase;
import net.pinkcats.NutUI.menu.architect.slots.SlotGhost;

public class NutItemMenu extends _SlotBase {


    public boolean lessThan;
    public Runnable onPacket;
    public int count = 1;
    private final BlockPos pos; // 添加 BlockPos 成员变量



  //  private final TransferBoxEntity transferEntity; // 添加 TransferEntity 成员变量
    public NutItemMenu(Inventory inventory, int Container_id, BlockPos pos) {
        super(MenuLib.ItemMenuRegiste.get(),Container_id,pos,null,9,0,9,false);
        //
        // super(Createlogistics.Transfer_Container.get(), Container_id, pos, CLBlock.LOGISTIC_BOX_PRIORITY.get(), 9, 0, 9,false);
        this.pos = pos;
        // 获取 TransferEntity 实例并赋值给成员变量
      //  if (inventory.player.level().getBlockEntity(pos) instanceof TransferBoxEntity transferEntity) {

         //   this.transferEntity = transferEntity; // 保存 TransferEntity


            // Show Item Slot
            int startX = 0; // 初始X坐标（可根据你的GUI调整）
            int startY = 10; // 初始Y坐标（可根据你的GUI调整）
            int slotSpacing = 20; // 槽位间距（X方向，通常为20像素）
            int rowMax = 9; // 每行最大槽位数量

            int currentX = startX;
            int currentY = startY;

            // 遍历所有槽位
           // for (int k = 0; k < transferEntity.getItems().getSlots(); k++) {
                // 添加当前槽位（使用当前坐标）
           //     addSlot(new SlotItemHandler(transferEntity.getItems(), k, currentX, currentY));

                // 计算下一个槽位的X坐标（当前X + 间距）
            //    currentX += slotSpacing;

                // 判断是否需要换行：如果是第9、18、27...个槽位（索引为8、17、26...）
                // 即 (k + 1) 能被 rowMax 整除时（因为索引从0开始）
           //     if ((k + 1) % rowMax == 0) {
           //         currentX = startX; // 重置X坐标到行首
            //        currentY += slotSpacing; // Y坐标增加，换到下一行
            ///    }
          //  }


            // Show Filter Slot
           // initFilterGhostSlots(); // 初始化过滤幽灵槽

    //    } else {
      //      this.transferEntity = null; // 如果没有找到 TransferEntity，设置为 null
    //    }

        //Add player slot
        addPlayerInventory(inventory, 0, 85+35);




    }

    //private void initFilterGhostSlots() {
        ///IItemHandler filterHandler = transferEntity.getFilterGhost();
      //  int slotX = 0; // 起始X坐标（与物品槽对齐）
      //  int slotY = 80; // 幽灵槽Y坐标（低于物品槽）
       // int slotSpacing = 20;

       // for (int i = 0; i < filterHandler.getSlots(); i++) {
        //    // 添加幽灵槽（使用自定义GhostSlot类）
       //     this.addSlot(new GhostSlot(filterHandler, i, slotX + (i * slotSpacing), slotY));
       // }
   // }



    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // 1. 先判断点击的槽位是否存在（slotId 超出范围则跳过）
        if (slotId < 0 || slotId >= this.slots.size()) {
            super.clicked(slotId, button, clickType, player); // 处理非槽位点击（如按钮）
            return;
        }

        // 2. 获取被点击的槽位
        Slot clickedSlot = this.slots.get(slotId);

        // 3. 判断是否为幽灵槽：如果是，执行自定义逻辑；否则走默认逻辑
        if (clickedSlot instanceof SlotGhost) {
            handleGhostSlotClick((SlotGhost) clickedSlot, player, clickType,button);
        } else {
            // 普通槽（如物品槽、玩家背包槽），走父类默认处理
            super.clicked(slotId, button, clickType, player);
        }
    }



    private void handleGhostSlotClick(SlotGhost slotGhost, Player player, ClickType clickType, int button) {
        // 获取玩家手持物品
        //    Used in the future ItemStack playerHandStack = player.getMainHandItem();
        // 获取幽灵槽的物品处理器（即 TransferBoxEntity 的 getFilterGhost()）
        IItemHandler filterHandler = slotGhost.getItemHandler();

        ItemStack playerHandStack = this.getCarried();

        // 获取幽灵槽在处理器中的索引
        int slotIndex = slotGhost.getSlotIndex();

        //if (player.level().isClientSide){
        //    mes.warn("Slot index: " + slotIndex);
        //    mes.warn("Slot handler stack: " + playerHandStack);
        //   mes.warn("Filter handler stack: " + filterHandler);
        //    mes.warn("Click handler stack: " + button);
        // }


        // 左键点击：设置幽灵槽为手持物品（复制，不消耗玩家物品）
        if (button == 0) {
            if (!playerHandStack.isEmpty()) {
                ItemStack ghostStack = playerHandStack.copy();
                ghostStack.setCount(1); // 幽灵槽只存1个
                filterHandler.insertItem(slotIndex, ghostStack, false); // 写入幽灵槽
                //transferEntity.setFilterGhostSlot(slotIndex, ghostStack);
                mes.warn("Ghost update");

            }
        }
        // 右键点击：清空幽灵槽
        else if (button == 1) {
            filterHandler.extractItem(slotIndex, 1, false); // 清空当前幽灵槽
        }

        // 标记菜单数据已更改，触发同步（可选，确保客户端显示更新）
        this.broadcastChanges();
    }



    private void updateGui() {
        if(onPacket != null)onPacket.run();
    }



    public BlockPos getPos() {
        return pos;
    }


    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

  //  public TransferBoxEntity getTransferEntity() {
   //     return transferEntity;
   // }

   // public TransferBoxEntity getTransferEntity(Inventory inventory, BlockPos pos) {
   //     if (inventory.player.level().getBlockEntity(pos) instanceof TransferBoxEntity transferEntity) {
   //         return transferEntity;
    //    }
   //     return null;
  //  }
}
