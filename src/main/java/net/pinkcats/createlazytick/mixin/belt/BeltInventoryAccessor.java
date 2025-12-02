package net.pinkcats.createlazytick.mixin.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BeltInventory.class)
public interface BeltInventoryAccessor {
    @Accessor("beltMovementPositive")
    boolean getBeltMovementPositive();

    @Accessor("belt")
    BeltBlockEntity getBelt();

    @Accessor("toInsert")
    List<TransportedItemStack> getToInsert();

    @Accessor("toRemove")
    List<TransportedItemStack> getToRemove();

    @Accessor("items")
    List<TransportedItemStack> getItems();



}
