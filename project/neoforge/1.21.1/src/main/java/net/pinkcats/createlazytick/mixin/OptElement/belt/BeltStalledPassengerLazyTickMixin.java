package net.pinkcats.createlazytick.mixin.OptElement.belt;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.config.ServerConfig;
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
 * entities every tick. Only a living entity which has already failed to move twice is sampled
 * here; players, item transport, and a passenger that begins moving keep Create's exact rate.
 */
@Mixin(value = BeltBlockEntity.class, remap = false)
public abstract class BeltStalledPassengerLazyTickMixin {

    @Shadow(remap = false) protected Map<Entity, TransportedEntityInfo> passengers;

    @Shadow public abstract Level getLevel();

    @Unique
    private final Map<Entity, PassengerState> createLazyTick$stalledPassengers = new WeakHashMap<>();

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler;transportEntity(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;Lnet/minecraft/world/entity/Entity;Lcom/simibubi/create/content/kinetics/belt/transport/BeltMovementHandler$TransportedEntityInfo;)V"
            ),
            remap = false
    )
    private void createLazyTick$throttleStalledPassenger(BeltBlockEntity belt, Entity passenger,
                                                           TransportedEntityInfo info) {
        Level level = passenger.level();
        if (level.isClientSide || !ServerConfig.getEnableLazyBeltStalledPassengers()
                || !(passenger instanceof LivingEntity) || passenger instanceof Player) {
            createLazyTick$stalledPassengers.remove(passenger);
            BeltMovementHandler.transportEntity(belt, passenger, info);
            return;
        }

        long now = level.getGameTime();
        PassengerState state = createLazyTick$stalledPassengers.computeIfAbsent(passenger, ignored -> new PassengerState());
        if (state.stationaryAttempts >= 2 && now < state.nextAttemptTick) {
            return;
        }

        Vec3 before = passenger.position();
        BeltMovementHandler.transportEntity(belt, passenger, info);
        if (passenger.position().distanceToSqr(before) <= 1.0E-8D) {
            state.stationaryAttempts++;
            if (state.stationaryAttempts >= 2) {
                state.nextAttemptTick = now + ServerConfig.getBeltStalledPassengerInterval();
            }
        } else {
            state.stationaryAttempts = 0;
            state.nextAttemptTick = now;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void createLazyTick$pruneFormerPassengers(CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || level.isClientSide || createLazyTick$stalledPassengers.isEmpty()
                || (level.getGameTime() & 31L) != 0L) {
            return;
        }
        createLazyTick$stalledPassengers.keySet().removeIf(entity -> passengers == null || !passengers.containsKey(entity));
    }

    @Unique
    private static final class PassengerState {
        private int stationaryAttempts;
        private long nextAttemptTick;
    }
}
