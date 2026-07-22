package net.pinkcats.createlazytick.bridge.Basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class BasinStateSnapshot {
    private final Object2IntMap<Item> itemQuantities;
    private boolean hasNbtSensitiveItems;
    private int fluidHash;
    private final ItemStack filterSnapshot;
    private final boolean outputBufferEmpty; // true = 无堵塞

    private final BlazeBurnerBlock.HeatLevel heatLevel;

    public BasinStateSnapshot(BasinBlockEntity basin) {
        this.itemQuantities = new Object2IntOpenHashMap<>();
        this.hasNbtSensitiveItems = false;
        this.fluidHash = 1;

        extractInventory(basin.getInputInventory());
        extractInventory(basin.getOutputInventory());

        if (basin.getTanks() != null) {
            basin.getTanks().forEach(this::extractFluidTank);
        }

        if (basin.getFilter() != null && basin.getFilter().getFilter() != null) {
            this.filterSnapshot = basin.getFilter().getFilter().copy();
        } else {
            this.filterSnapshot = ItemStack.EMPTY;
        }

        if (basin instanceof IBasinOptimization opt) {
            this.heatLevel = opt.clt$getHeatLevel();
            this.outputBufferEmpty = opt.clt$isOutputBufferEmpty();
        } else {
            this.heatLevel = BlazeBurnerBlock.HeatLevel.NONE;
            this.outputBufferEmpty = true;
        }
    }

    private void extractInventory(SmartInventory inv) {
        if (inv == null) return;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            this.itemQuantities.put(item, this.itemQuantities.getInt(item) + stack.getCount());

            if (!stack.getComponentsPatch().isEmpty() && BasinRecipeIndex.isNbtSensitive(item)) {//<-
                this.hasNbtSensitiveItems = true;
            }
        }
    }

    private void extractFluidTank(SmartFluidTankBehaviour tankBehaviour) {
        if (tankBehaviour == null) return;
        for (int i = 0; i < tankBehaviour.getTanks().length; i++) {
            FluidStack fs = tankBehaviour.getPrimaryHandler().getFluidInTank(i);
            if (!fs.isEmpty()) {
                int typeHash = fs.getFluid().hashCode();
                int tagHash = !fs.getComponentsPatch().isEmpty() ? fs.getComponentsPatch().hashCode() : 0;
                this.fluidHash = 31 * this.fluidHash + typeHash * 31 + fs.getAmount() + tagHash;
            }
        }
    }

    public Object2IntMap<Item> getItemQuantities() { return itemQuantities; }
    public boolean hasNbtSensitiveItems() { return hasNbtSensitiveItems; }
    public int getFluidHash() { return fluidHash; }
    public ItemStack getFilterSnapshot() { return filterSnapshot; }

    // Getters
    public boolean isOutputBufferEmpty() { return outputBufferEmpty; }
    public BlazeBurnerBlock.HeatLevel getHeatLevel() { return heatLevel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasinStateSnapshot that = (BasinStateSnapshot) o;

        if (fluidHash != that.fluidHash) return false;
        if (outputBufferEmpty != that.outputBufferEmpty) return false; // 比较输出
        if (heatLevel != that.heatLevel) return false;               // 比较热量
        if (hasNbtSensitiveItems != that.hasNbtSensitiveItems) return false;
        if (!ItemStack.isSameItemSameComponents(filterSnapshot, that.filterSnapshot)) return false;
        return itemQuantities.equals(that.itemQuantities);
    }

    // For debug
    @Override
    public String toString() {
        return "Snap{" +
                "Items=" + itemQuantities.size() +
                ", FluidH=" + fluidHash +
                ", OutH=" + outputBufferEmpty +
                ", Heat=" + heatLevel +
                '}';
    }
}