package net.pinkcats.createlazytick.mixin.OptElement.itemdrain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ItemDrainBlockEntity.class,remap = false)
public interface ItemDrainAccessor {
    @Accessor(value = "heldItem")
    TransportedItemStack getHeldItem();

    @Accessor(value = "heldItem")
    void setHeldItem(TransportedItemStack stack);

    @Accessor(value = "processingTicks")
    int getProcessingTicks();

    @Accessor(value = "processingTicks")
    void setProcessingTicks(int ticks);

    @Invoker(value = "continueProcessing")
    boolean invokeContinueProcessing();
}