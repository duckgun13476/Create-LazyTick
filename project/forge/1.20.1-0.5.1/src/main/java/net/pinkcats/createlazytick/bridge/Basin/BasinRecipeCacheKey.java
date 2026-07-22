package net.pinkcats.createlazytick.bridge.Basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Objects;

public final class BasinRecipeCacheKey {
    private final Object2IntMap<Item> inputItemQuantities;
    private final boolean hasNbtSensitiveItems;
    private final int fluidHash;
    private final ItemStack filterSnapshot;
    private final BlazeBurnerBlock.HeatLevel heatLevel;

    public BasinRecipeCacheKey(BasinBlockEntity basin) {
        this.inputItemQuantities = new Object2IntOpenHashMap<>();
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
                Item item = stack.getItem();
                inputItemQuantities.put(item, inputItemQuantities.getInt(item) + stack.getCount());
                if (stack.hasTag() && BasinRecipeIndex.isNbtSensitive(item)) {
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
                    hash = 31 * hash + typeHash * 31 + fs.getAmount() + tagHash;
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
        return Objects.equals(inputItemQuantities, that.inputItemQuantities);
    }

    @Override
    public int hashCode() {
        int result = inputItemQuantities.hashCode();
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

    @Override
    public String toString() {
        return "Key{" +
                "items=" + inputItemQuantities +
                ", nbt=" + hasNbtSensitiveItems +
                ", fluidHash=" + fluidHash +
                ", filter=" + filterSnapshot +
                ", heat=" + heatLevel +
                '}';
    }
}
