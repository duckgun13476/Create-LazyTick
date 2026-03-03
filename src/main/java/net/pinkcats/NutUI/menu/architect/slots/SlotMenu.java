package net.pinkcats.NutUI.menu.architect.slots;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public abstract class SlotMenu extends AbstractContainerMenu {


    private final BlockPos pos;
    private final Block targetblock;

    protected SlotMenu(@Nullable MenuType<?> pMenuType, int Container_id, BlockPos pos, Block targetblock) {
        super(pMenuType, Container_id);
        this.pos = pos;
        this.targetblock = targetblock;
    }


    public int ASlotAdd(IItemHandler Items, int index , int x, int y){
        addSlot(new SlotItemHandler(Items,index,x,y));
        return index;
    }


    public int addSlotLine(Container container , int index , int x, int y, int amount, int dx) {
        for (int k = 0; k < amount; k++) {
            addSlot(new Slot(container,index,x,y));
            x += dx;
            index ++;

        }
        return index;

    }
    public int addSlotBox(Container container, int index , int x, int y,int X_amount, int dx,int Y_amount,int dy) {
        for (int k = 0; k < Y_amount; k++) {
            index = addSlotLine(container,index,x,y,X_amount,dx);
            y += dy;
        }
        return index;
    }


    /**
     * Add Player's Inventory
     */
    public void addPlayerInventory(Container playerInventory, int x, int y) {
        addSlotBox(playerInventory,9,x,y,9,20,3,19);
        y += 56;
        addSlotLine(playerInventory,0,x,y,9,20);
    }



    /**
     Tool func
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(pPlayer.level(),pos),pPlayer,targetblock);
    }


}
