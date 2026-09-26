package net.pinkcats.NutUI.menu.architect.slots;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

// 幽灵槽实现（不可放置物品，仅作过滤显示）
public class SlotGhost extends Slot {
    private final Container itemHandler;

    public SlotGhost(Container itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
    }

    public Container getItemHandler() { return itemHandler; }

    // 禁止放入物品
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    // 禁止取出物品（如果需要允许清空，可重写为 return true）
    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    // 渲染为半透明（可选，需配合客户端渲染）
    @Override
    public int getMaxStackSize() {
        return 1; // 幽灵槽通常只显示一个物品作为过滤规则
    }
}
