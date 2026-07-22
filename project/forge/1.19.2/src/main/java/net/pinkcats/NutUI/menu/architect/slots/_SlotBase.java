package net.pinkcats.NutUI.menu.architect.slots;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class _SlotBase extends SlotMenu {

    private final int SlotCount,SlotNormal,SlotNormalCount;
    private boolean IsFake;

    public static final int PLAYER_SLOT_COUNT = 27;
    public static final int PLAYER_HOT_BAR_SLOT_COUNT = 9;

    public _SlotBase(@Nullable MenuType<?> pMenuType, int Container_id, BlockPos pos, Block targetblock,
                     int SlotCount, int SlotNormal, int SlotNormalCount, boolean IsFake) {
        super(pMenuType, Container_id, pos, targetblock);
        this.SlotCount = SlotCount;
        this.SlotNormal = SlotNormal;
        this.SlotNormalCount = SlotNormalCount;
        this.IsFake = false;
    }


    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int pIndex){
        var itemstack = ItemStack.EMPTY;
        if (isFake())
            return itemstack;

        var slot = this.getSlot(pIndex);
        if(slot.hasItem()){
            var stack = slot.getItem();
            itemstack = stack.copy();
            if (pIndex < SlotCount){
                if(!this.moveItemStackTo(stack,SlotCount,this.slots.size(),true)) return ItemStack.EMPTY;

            }
            else if (!this.moveItemStackTo(stack,SlotNormal,SlotNormalCount,true)) return ItemStack.EMPTY;

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);

        }
        return itemstack;
    }

    public boolean isFake() {
        return IsFake;
    }
}


