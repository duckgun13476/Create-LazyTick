package net.pinkcats.createlazytick.mixin.itemdrain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ItemDrainBlockEntity.class,remap = false)
public interface ItemDrainAccessor {
    @Accessor("heldItem")
    TransportedItemStack getHeldItem();

    @Accessor("heldItem")
    void setHeldItem(TransportedItemStack stack);

    @Accessor("processingTicks")
    int getProcessingTicks();

    @Accessor("processingTicks")
    void setProcessingTicks(int ticks);

    @Invoker("continueProcessing")
    boolean invokeContinueProcessing();
}