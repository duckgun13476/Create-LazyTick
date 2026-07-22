package net.pinkcats.NutUI.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;
import net.pinkcats.NutUI.menu.architect.slots.SlotGhost;
import net.pinkcats.NutUI.menu.architect.slots._SlotBase;
import net.pinkcats.createlazytick.Gui.mes;

import java.util.HashMap;
import java.util.Map;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class NutKineticMenu {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<MenuType<NutItemMenu>> ItemMenuRegiste =
            MENU_TYPES.register("nut_item_menu", () ->
                    IForgeMenuType.create((windowId, inventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        ResourceLocation menuId = buf.readResourceLocation();
                        return new NutItemMenu(inventory, windowId, pos, menuId);
                    })
            );

    public static void init(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

    public static class NutItemMenu extends _SlotBase {

        private final ResourceLocation menuId;
        public boolean lessThan;
        public Runnable onPacket;
        public int count = 1;
        private final BlockPos pos;
        private final Map<String, Object> customServerVariables = new HashMap<>();

        public NutItemMenu(Inventory inventory, int containerId, BlockPos pos, ResourceLocation menuId) {
            super(ItemMenuRegiste.get(), containerId, pos, null, 9, 0, 9, false);
            this.pos = pos;
            this.menuId = menuId;
            RenderPlayerInventory(inventory);
        }


        protected void RenderPlayerInventory(Inventory inventory) {
            NutMenuInfo.data data = NutMenuInfo.get(this.menuId);
            if (data == null || !data.hasPlayerInventory()) {
                return;
            }
            addPlayerInventory(inventory, data.playerInventoryX(), data.playerInventoryY());
        }


        public ResourceLocation getMenuId() {
            return menuId;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (slotId < 0 || slotId >= this.slots.size()) {
                super.clicked(slotId, button, clickType, player);
                return;
            }

            Slot clickedSlot = this.slots.get(slotId);
            if (clickedSlot instanceof SlotGhost) {
                handleGhostSlotClick((SlotGhost) clickedSlot, button);
            } else {
                super.clicked(slotId, button, clickType, player);
            }
        }

        private void updateGui() {
            if (onPacket != null) {
                onPacket.run();
            }
        }

        public void handleClientAction(ServerPlayer player, String action, Map<String, Object> variables) {
            if (action == null || action.isBlank()) {
                return;
            }

            if ("set_key".equals(action)) {
                String key = variables == null ? null : String.valueOf(variables.get("key"));
                Object value = variables == null ? null : variables.get("value");
                applyBuiltInKeyUpdate(key, value);
                return;
            }

            handleCustomClientAction(player, action, variables);
        }

        protected void handleCustomClientAction(ServerPlayer player, String action, Map<String, Object> variables) {
        }

        protected void applyBuiltInKeyUpdate(String key, Object value) {
            if (key == null) {
                return;
            }
            switch (key) {
                case "count" -> this.count = asInt(value, this.count);
                case "lessThan" -> this.lessThan = asBoolean(value, this.lessThan);
                default -> customServerVariables.put(key, value);
            }
        }

        public Map<String, Object> buildAutoSyncVariables() {
            Map<String, Object> variables = new HashMap<>();
            appendBaseAutoSyncVariables(variables);
            appendAutoSyncVariables(variables);
            return variables;
        }

        protected void appendBaseAutoSyncVariables(Map<String, Object> variables) {
            variables.put("count", count);
            variables.put("lessThan", lessThan);
            variables.put("menuId", menuId.toString());
            variables.put("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});

            if (!customServerVariables.isEmpty()) {
                for (Map.Entry<String, Object> entry : customServerVariables.entrySet()) {
                    String key = entry.getKey();
                    if ("count".equals(key) || "lessThan".equals(key) || "menuId".equals(key) || "pos".equals(key)) {
                        continue;
                    }
                    variables.put(key, entry.getValue());
                }
            }
        }

        protected void appendAutoSyncVariables(Map<String, Object> variables) {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        public BlockPos getPos() {
            return pos;
        }

        private void handleGhostSlotClick(SlotGhost slotGhost, int button) {
            IItemHandler filterHandler = slotGhost.getItemHandler();
            ItemStack playerHandStack = this.getCarried();
            int slotIndex = slotGhost.getSlotIndex();

            if (button == 0) {
                if (!playerHandStack.isEmpty()) {
                    ItemStack ghostStack = playerHandStack.copy();
                    ghostStack.setCount(1);
                    filterHandler.insertItem(slotIndex, ghostStack, false);
                    mes.warn("Ghost update");
                }
            } else if (button == 1) {
                filterHandler.extractItem(slotIndex, 1, false);
            }

            this.broadcastChanges();
        }




        private static int asInt(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException ignored) {
                }
            }
            return fallback;
        }




        private static boolean asBoolean(Object value, boolean fallback) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String str) {
                return Boolean.parseBoolean(str);
            }
            return fallback;
        }



    }
}
