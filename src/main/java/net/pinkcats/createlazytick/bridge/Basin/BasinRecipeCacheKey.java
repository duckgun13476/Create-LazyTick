package net.pinkcats.createlazytick.bridge.Basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class BasinRecipeCacheKey {
    private final Set<Item> inputItems;
    private final boolean hasNbtSensitiveItems;
    private final int fluidHash;
    private final ItemStack filterSnapshot;
    private final BlazeBurnerBlock.HeatLevel heatLevel;

    public BasinRecipeCacheKey(BasinBlockEntity basin) {
        this.inputItems = new HashSet<>();
        this.filterSnapshot = basin.getFilter() != null && basin.getFilter().getFilter() != null
                ? basin.getFilter().getFilter().copy()
                : ItemStack.EMPTY;
        this.heatLevel = basin instanceof IBasinOptimization opt
                ? opt.clt$getHeatLevel()
                : BlazeBurnerBlock.HeatLevel.NONE;

        boolean nbtSensitive = false;
        SmartInventory inputInventory = basin.getInputInventory();
        if (inputInventory != null) {
            for (int i = 0; i < inputInventory.getSlots(); i++) {
                ItemStack stack = inputInventory.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                inputItems.add(stack.getItem());
                if (stack.hasTag() && BasinRecipeIndex.isNbtSensitive(stack.getItem())) {
                    nbtSensitive = true;
                }
            }
        }
        this.hasNbtSensitiveItems = nbtSensitive;

        int hash = 1;
        if (basin.getTanks() != null) {
            for (SmartFluidTankBehaviour tankBehaviour : basin.getTanks()) {
                if (tankBehaviour == null) continue;
                for (int i = 0; i < tankBehaviour.getTanks().length; i++) {
                    FluidStack fs = tankBehaviour.getPrimaryHandler().getFluidInTank(i);
                    if (fs.isEmpty()) continue;
                    int typeHash = fs.getFluid().hashCode();
                    int tagHash = fs.hasTag() ? fs.getTag().hashCode() : 0;
                    hash = 31 * hash + typeHash * 31 + tagHash;
                }
            }
        }
        this.fluidHash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasinRecipeCacheKey that)) return false;
        if (hasNbtSensitiveItems != that.hasNbtSensitiveItems) return false;
        if (fluidHash != that.fluidHash) return false;
        if (heatLevel != that.heatLevel) return false;
        if (!ItemStack.matches(filterSnapshot, that.filterSnapshot)) return false;
        return Objects.equals(inputItems, that.inputItems);
    }

    @Override
    public int hashCode() {
        int result = inputItems.hashCode();
        result = 31 * result + Boolean.hashCode(hasNbtSensitiveItems);
        result = 31 * result + fluidHash;
        result = 31 * result + clt$hashItemStack(filterSnapshot);
        result = 31 * result + heatLevel.hashCode();
        return result;
    }

    private static int clt$hashItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int result = Item.getId(stack.getItem());
        result = 31 * result + stack.getCount();
        result = 31 * result + (stack.hasTag() ? stack.getTag().hashCode() : 0);
        return result;
    }
}
