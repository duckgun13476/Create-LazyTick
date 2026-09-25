package net.pinkcats.createlazytick.mixin.belt;

import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TransportedEntityInfo.class, remap = false)
public interface TransportedEntityInfoAccessor {
    @Accessor("ticksSinceLastCollision") int createLazyTick$getTicksSinceLastCollision();
    @Accessor("ticksSinceLastCollision") void createLazyTick$setTicksSinceLastCollision(int ticks);
    @Accessor("lastCollidedState") BlockState createLazyTick$getLastCollidedState();
}
