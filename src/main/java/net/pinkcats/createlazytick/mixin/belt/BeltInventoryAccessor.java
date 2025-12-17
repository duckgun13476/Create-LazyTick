package net.pinkcats.createlazytick.mixin.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BeltInventory.class,remap = false)
public interface BeltInventoryAccessor {
    @Accessor(value = "beltMovementPositive",remap = false)
    boolean getBeltMovementPositive();

    @Accessor(value = "belt",remap = false)
    BeltBlockEntity getBelt();

    @Accessor(value = "toInsert",remap = false)
    List<TransportedItemStack> getToInsert();

    @Accessor(value = "toRemove",remap = false)
    List<TransportedItemStack> getToRemove();

    @Accessor(value = "items",remap = false)
    List<TransportedItemStack> getItems();



}
