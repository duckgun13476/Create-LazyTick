package net.pinkcats.createlazytick.mixin.OptElement.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BeltInventory.class,remap = false)
public interface BeltInventoryAccessor {
    @Accessor(value = "beltMovementPositive")
    boolean getBeltMovementPositive();

    @Accessor(value = "belt")
    BeltBlockEntity getBelt();

    @Accessor(value = "toInsert")
    List<TransportedItemStack> getToInsert();

    @Accessor(value = "toRemove")
    List<TransportedItemStack> getToRemove();

    @Accessor(value = "items")
    List<TransportedItemStack> getItems();



}
