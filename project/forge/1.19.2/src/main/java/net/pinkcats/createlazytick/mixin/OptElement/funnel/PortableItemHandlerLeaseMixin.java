package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import net.pinkcats.createlazytick.bridge.Create.IPortableInterfaceLease;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity$InterfaceItemHandler", remap = false)
public abstract class PortableItemHandlerLeaseMixin implements IPortableInterfaceLease.Source {
    @Shadow @Final PortableItemInterfaceBlockEntity this$0;

    @Override
    public IPortableInterfaceLease createLazyTick$getLeaseOwner() {
        return (IPortableInterfaceLease) this$0;
    }
}
