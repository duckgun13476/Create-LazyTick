package net.pinkcats.createlazytick.mixin.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.fabric.FabricServerConfig;
import net.pinkcats.createlazytick.optimization.belt.BeltStalledPassengerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

/** Port of the tested Forge 1.20.1 blocked-passenger cadence, scoped to living non-player entities. */
@Mixin(value = BeltBlockEntity.class, remap = false)
public abstract class BeltStalledPassengerLazyTickMixin {
    @Shadow(remap = false) public Map<Entity, TransportedEntityInfo> passengers;
    @Unique private final Map<Entity, BeltStalledPassengerState> createLazyTick$stalledPassengers = new WeakHashMap<>();
    @Unique private int createLazyTick$passengerPruneTicks;

    // Descriptor is intentionally omitted: Fabric remaps Entity between named dev and intermediary runtime.
    @Redirect(method = "lambda$tick$0", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler;transportEntity"), remap = false)
    private void createLazyTick$throttleStalledPassenger(BeltBlockEntity belt, Entity passenger, TransportedEntityInfo info) {
        var level = passenger.level();
        if (level.isClientSide || !FabricServerConfig.enableStalledPassengers() || !(passenger instanceof LivingEntity) || passenger instanceof Player) {
            createLazyTick$stalledPassengers.remove(passenger);
            BeltMovementHandler.transportEntity(belt, passenger, info);
            return;
        }
        long now = level.getGameTime();
        BeltStalledPassengerState state = createLazyTick$stalledPassengers.computeIfAbsent(passenger, ignored -> new BeltStalledPassengerState());
        TransportedEntityInfoAccessor accessor = (TransportedEntityInfoAccessor) info;
        BlockState beltState = accessor.createLazyTick$getLastCollidedState();
        boolean blockedByEntity = false;
        if (Math.abs(belt.getBeltMovementSpeed()) < .5f) {
            Direction.Axis axis = beltState.getValue(BeltBlock.HORIZONTAL_FACING).getAxis();
            Direction direction = Direction.get(axis == Direction.Axis.X ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
            Vec3 distance = Vec3.atLowerCornerOf(direction.getNormal()).scale(.5);
            AABB box = passenger.getBoundingBox().move(distance).inflate(-Math.abs(distance.x), -Math.abs(distance.y), -Math.abs(distance.z));
            blockedByEntity = !level.getEntities(passenger, box, other -> !BeltMovementHandler.shouldIgnoreBlocking(passenger, other)).isEmpty();
        }
        if (state.stationaryAttempts() >= 2 && now < state.nextAttemptTick() && blockedByEntity) {
            accessor.createLazyTick$setTicksSinceLastCollision(accessor.createLazyTick$getTicksSinceLastCollision() - 1);
            return;
        }
        BeltMovementHandler.transportEntity(belt, passenger, info);
        if (blockedByEntity) state.recordCollision(now + FabricServerConfig.stalledPassengerInterval());
        else state.recordMovement(now);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void createLazyTick$pruneFormerPassengers(CallbackInfo ci) {
        if (createLazyTick$stalledPassengers.isEmpty() || (createLazyTick$passengerPruneTicks++ & 31) != 0) return;
        createLazyTick$stalledPassengers.keySet().removeIf(entity -> passengers == null || !passengers.containsKey(entity));
    }
}
