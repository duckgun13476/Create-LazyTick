package net.pinkcats.NutUI.menu.architect.slots;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

// 幽灵槽实现（不可放置物品，仅作过滤显示）
public class SlotGhost extends SlotItemHandler {
    public SlotGhost(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    // 禁止放入物品
    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    // 禁止取出物品（如果需要允许清空，可重写为 return true）
    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }

    // 渲染为半透明（可选，需配合客户端渲染）
    @Override
    public int getMaxStackSize() {
        return 1; // 幽灵槽通常只显示一个物品作为过滤规则
    }
}
