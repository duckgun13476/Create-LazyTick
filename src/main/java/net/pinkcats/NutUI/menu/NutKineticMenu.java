package net.pinkcats.NutUI.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.pinkcats.NutUI.menu.architect.slots.SlotGhost;
import net.pinkcats.NutUI.menu.architect.slots._SlotBase;
import net.pinkcats.createlazytick.Gui.mes;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class NutKineticMenu {


    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES,MODID);


    public static final RegistryObject<MenuType<NutItemMenu>> ItemMenuRegiste =
            MENU_TYPES.register("nut_item_menu", () ->
                    IForgeMenuType.create((windowId, inventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        ResourceLocation menuId = buf.readResourceLocation();
                        return new NutItemMenu(inventory, windowId, pos, menuId);
                    })
            );

    //Only Entrance
    public static void init(IEventBus IEventBus) {
        MENU_TYPES.register(IEventBus);
    }

    public static class NutItemMenu extends _SlotBase {

        private final ResourceLocation menuId;
        public boolean lessThan;
        public Runnable onPacket;
        public int count = 1;
        private final BlockPos pos; // 添加 BlockPos 成员变量


        public NutItemMenu(Inventory inventory, int Container_id, BlockPos pos, ResourceLocation menuId) {
            super(ItemMenuRegiste.get(), Container_id, pos, null, 9, 0, 9, false);
            this.pos = pos;
            this.menuId = menuId; // ✅ 保存
            addPlayerInventory(inventory, 0, 85+35);
        }

        public ResourceLocation getMenuId() {
            return menuId;
        }


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

    }
}
