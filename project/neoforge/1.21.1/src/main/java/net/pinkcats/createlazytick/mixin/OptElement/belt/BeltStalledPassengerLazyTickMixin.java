package net.pinkcats.createlazytick.mixin.OptElement.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.config.ServerConfig;
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

/**
 * The vanilla/Create passenger path is intentionally separate from BeltInventory: it moves
 * entities every tick. Only a living entity which has already collided twice is sampled
 * here; players, item transport, and a passenger that begins moving keep Create's exact rate.
 */
@Mixin(value = BeltBlockEntity.class, remap = false)
public abstract class BeltStalledPassengerLazyTickMixin {

    @Shadow(remap = false) protected Map<Entity, TransportedEntityInfo> passengers;

    @Unique
    private final Map<Entity, BeltStalledPassengerState> createLazyTick$stalledPassengers = new WeakHashMap<>();

    @Unique
    private int createLazyTick$passengerPruneTicks;

    @Redirect(
            // Create performs the passenger iteration in this compiled lambda, not in tick itself.
            method = "lambda$tick$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler;transportEntity(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;Lnet/minecraft/world/entity/Entity;Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler$TransportedEntityInfo;)V"
            ),
            remap = false
    )
    private void createLazyTick$throttleStalledPassenger(BeltBlockEntity belt, Entity passenger,
                                                           TransportedEntityInfo info) {
        var level = passenger.level();
        if (level.isClientSide || !ServerConfig.getEnableLazyBeltStalledPassengers()
                || !(passenger instanceof LivingEntity) || passenger instanceof Player) {
            createLazyTick$stalledPassengers.remove(passenger);
            BeltMovementHandler.transportEntity(belt, passenger, info);
            return;
        }

        long now = level.getGameTime();
        BeltStalledPassengerState state = createLazyTick$stalledPassengers.computeIfAbsent(
                passenger, ignored -> new BeltStalledPassengerState());
        TransportedEntityInfoAccessor accessor = (TransportedEntityInfoAccessor) info;
        BlockState beltState = accessor.createLazyTick$getLastCollidedState();
        float movementSpeed = belt.getBeltMovementSpeed();
        boolean blockedByEntity = false;
        if (Math.abs(movementSpeed) < .5f) {
            Direction.Axis axis = beltState.getValue(BeltBlock.HORIZONTAL_FACING).getAxis();
            Direction movementDirection = Direction.get(axis == Direction.Axis.X ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
            Vec3 checkDistance = Vec3.atLowerCornerOf(movementDirection.getNormal()).scale(.5);
            AABB box = passenger.getBoundingBox().move(checkDistance)
                    .inflate(-Math.abs(checkDistance.x), -Math.abs(checkDistance.y), -Math.abs(checkDistance.z));
            blockedByEntity = !level.getEntities(passenger, box,
                    other -> !BeltMovementHandler.shouldIgnoreBlocking(passenger, other)).isEmpty();
        }

        if (state.stationaryAttempts() >= 2 && now < state.nextAttemptTick() && blockedByEntity) {
            // Keep the passenger admission alive while avoiding the expensive full movement
            // path. The bounded AABB probe is deliberately retained on every skipped tick so
            // deleting or moving the leading entity wakes the passenger immediately.
            accessor.createLazyTick$setTicksSinceLastCollision(
                    accessor.createLazyTick$getTicksSinceLastCollision() - 1);
            return;
        }

        BeltMovementHandler.transportEntity(belt, passenger, info);
        if (blockedByEntity) {
            state.recordCollision(now + ServerConfig.getBeltStalledPassengerInterval());
        } else {
            state.recordMovement(now);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void createLazyTick$pruneFormerPassengers(CallbackInfo ci) {
        if (createLazyTick$stalledPassengers.isEmpty()
                || (createLazyTick$passengerPruneTicks++ & 31) != 0) {
            return;
        }
        createLazyTick$stalledPassengers.keySet().removeIf(entity -> passengers == null || !passengers.containsKey(entity));
    }
}
