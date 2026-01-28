package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FluidReactions;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

//记得补偿流体(目前速度有异常)
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public class FluidLazyTickMixin extends BlockEntityBehaviour {

    @Shadow
    public Map<Direction, PipeConnection> interfaces;

    @Shadow
    public FluidTransportBehaviour.UpdatePhase phase;

    @Shadow
    public static final BehaviourType<FluidTransportBehaviour> TYPE = new BehaviourType<>();

    @Shadow
    public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
        return true;
    }

    public FluidLazyTickMixin(SmartBlockEntity be) {
        super(be);
    }

    @Unique
    int pipeTick = 0;


    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFluid()) {
            return;
        }

        pipeTick++;
        if (pipeTick< ServerConfig.getFluidDelayMax())
        {
            ci.cancel();
            return;
        }
        pipeTick=0;


        super.tick();
        Level world = getWorld();
        BlockPos pos = getPos();
        boolean onServer = !world.isClientSide || blockEntity.isVirtual();

        if (interfaces == null) {
            ci.cancel();
            return;
        }
        Collection<PipeConnection> connections = interfaces.values();

        // Do not provide a lone pipe connection with its own flow input
        PipeConnection singleSource = null;

//		if (onClient) {
//			connections.forEach(connection -> {
//				connection.visualizeFlow(pos);
//				connection.visualizePressure(pos);
//			});
//		}

        if (phase == FluidTransportBehaviour.UpdatePhase.WAIT_FOR_PUMPS) {
            phase = FluidTransportBehaviour.UpdatePhase.FLIP_FLOWS;
            ci.cancel();
            return;
        }

        if (onServer) {
            boolean sendUpdate = false;
            for (PipeConnection connection : connections) {
                sendUpdate |= connection.flipFlowsIfPressureReversed();
                connection.manageSource(world, pos);
            }
            if (sendUpdate)
                blockEntity.notifyUpdate();
        }

        if (phase == FluidTransportBehaviour.UpdatePhase.FLIP_FLOWS) {
            phase = FluidTransportBehaviour.UpdatePhase.IDLE;
            ci.cancel();
            return;
        }

        if (onServer) {
            FluidStack availableFlow = FluidStack.EMPTY;
            FluidStack collidingFlow = FluidStack.EMPTY;

            for (PipeConnection connection : connections) {
                FluidStack fluidInFlow = connection.getProvidedFluid();
                if (fluidInFlow.isEmpty())
                    continue;
                if (availableFlow.isEmpty()) {
                    singleSource = connection;
                    availableFlow = fluidInFlow;
                    continue;
                }
                if (availableFlow.isFluidEqual(fluidInFlow)) {
                    singleSource = null;
                    availableFlow = fluidInFlow;
                    continue;
                }
                collidingFlow = fluidInFlow;
                break;
            }

            if (!collidingFlow.isEmpty()) {
                FluidReactions.handlePipeFlowCollision(world, pos, availableFlow, collidingFlow);
                ci.cancel();
                return;
            }

            boolean sendUpdate = false;
            for (PipeConnection connection : connections) {
                FluidStack internalFluid = singleSource != connection ? availableFlow : FluidStack.EMPTY;
                Predicate<FluidStack> extractionPredicate =
                        extracted -> canPullFluidFrom(extracted, blockEntity.getBlockState(), connection.side);

                sendUpdate |= connection.manageFlows(world, pos, internalFluid, extractionPredicate);


            }

            if (sendUpdate)
                blockEntity.notifyUpdate();
        }

        for (PipeConnection connection : connections)
            connection.tickFlowProgress(world, pos);

        ci.cancel();
    }


    /**
     * @author PinkCats
     * @reason for inject
     */
    @Overwrite
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
