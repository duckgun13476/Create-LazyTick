package net.pinkcats.createlazytick.mixin.itemdrain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemDrainBlockEntity.class)
public interface ItemDrainAccessor {
    @Accessor(value = "heldItem",remap = false)
    TransportedItemStack getHeldItem();

    @Accessor(value = "heldItem",remap = false)
    void setHeldItem(TransportedItemStack stack);

    @Accessor(value = "processingTicks",remap = false)
    int getProcessingTicks();

    @Accessor(value = "processingTicks",remap = false)
    void setProcessingTicks(int ticks);

    @Invoker(value = "continueProcessing",remap = false)
    boolean invokeContinueProcessing();
}