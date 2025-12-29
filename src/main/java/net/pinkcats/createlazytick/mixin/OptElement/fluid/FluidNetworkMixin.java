package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

import static net.pinkcats.createlazytick.Config.fluid_delay_max;

@Mixin(value = FluidNetwork.class,remap = false)
public class FluidNetworkMixin {

    @Shadow
    private static int CYCLES_PER_TICK = 16;
    @Shadow
    Level world;
    @Shadow
    BlockFace start;
    @Shadow
    Supplier<LazyOptional<IFluidHandler>> sourceSupplier;
    @Shadow
    LazyOptional<IFluidHandler> source;
    @Shadow
    int transferSpeed;
    @Shadow
    int pauseBeforePropagation;
    @Shadow
    List<BlockFace> queued;
    @Shadow
    Set<Pair<BlockFace, PipeConnection>> frontier;
    @Shadow
    Set<BlockPos> visited;
    @Shadow
    FluidStack fluid;
    @Shadow
    List<Pair<BlockFace, LazyOptional<IFluidHandler>>> targets;
    @Shadow
    Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;
    @Shadow
    private boolean isPresent(BlockFace location) {return false;}
    @Shadow
    private PipeConnection get(BlockFace location) {return null;}
    @Shadow
    private void keepPortableFluidInterfaceEngaged() {}

    /**
     * @author PinkCats
     * @reason rewrite fluid logic
     */
    @Overwrite
    public void tick() {
        if (pauseBeforePropagation > 1) {
            pauseBeforePropagation--;
            return;
        }

        for (int cycle = 0; cycle < CYCLES_PER_TICK; cycle++) {
            boolean shouldContinue = false;
            for (Iterator<BlockFace> iterator = queued.iterator(); iterator.hasNext();) {
                BlockFace blockFace = iterator.next();
                if (!isPresent(blockFace))
                    continue;
                PipeConnection pipeConnection = get(blockFace);
                if (pipeConnection != null) {
                    if (blockFace.equals(start))
                        transferSpeed = (int) Math.max(1, pipeConnection.getPressure().get(true) / 2f * fluid_delay_max);
                    frontier.add(Pair.of(blockFace, pipeConnection));
                }
                iterator.remove();
            }

//			drawDebugOutlines();

            for (Iterator<Pair<BlockFace, PipeConnection>> iterator = frontier.iterator(); iterator.hasNext();) {
                Pair<BlockFace, PipeConnection> pair = iterator.next();
                BlockFace blockFace = pair.getFirst();
                PipeConnection pipeConnection = pair.getSecond();

                if (!pipeConnection.hasFlow())
                    continue;

                PipeConnectionAccessor pressureAccessor = (PipeConnectionAccessor) pipeConnection;
                Optional<PipeConnection.Flow> PipFlow = pressureAccessor.getFlow();
                PipeConnection.Flow flow = PipFlow.get();



                if (!fluid.isEmpty() && !flow.fluid.isFluidEqual(fluid)) {
                    iterator.remove();
                    continue;
                }
                if (!flow.inbound) {
                    if (pipeConnection.comparePressure() >= 0)
                        iterator.remove();
                    continue;
                }
                if (!flow.complete)
                    continue;

                if (fluid.isEmpty())
                    fluid = flow.fluid;

                boolean canRemove = true;
                for (Direction side : Iterate.directions) {
                    if (side == blockFace.getFace())
                        continue;
                    BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
                    PipeConnection adjacent = get(adjacentLocation);

                    if (adjacent == null)
                        continue;
                    PipeConnectionAccessor adjacentAccessor = (PipeConnectionAccessor) adjacent;
                    Optional<PipeConnection.Flow> adjacentFlow = adjacentAccessor.getFlow();
                    Optional<FlowSource> adjacentSource = adjacentAccessor.getSource();

                    if (!adjacent.hasFlow()) {
                        // Branch could potentially still appear
                        if (adjacent.hasPressure() && adjacent.getPressure().getSecond() > 0)
                            canRemove = false;
                        continue;
                    }
                    PipeConnection.Flow outFlow = adjacentFlow.get();
                    if (outFlow.inbound) {
                        if (adjacent.comparePressure() > 0)
                            canRemove = false;
                        continue;
                    }
                    if (!outFlow.complete) {
                        canRemove = false;
                        continue;
                    }

                    // Give pipe end a chance to init connections
                    if (!adjacentSource.isPresent() && !adjacent.determineSource(world, blockFace.getPos())) {
                        canRemove = false;
                        continue;
                    }

                    if (adjacentSource.isPresent() && adjacentSource.get()
                            .isEndpoint()) {
                        targets.add(Pair.of(adjacentLocation, adjacentSource.get()
                                .provideHandler()));
                        continue;
                    }

                    if (visited.add(adjacentLocation.getConnectedPos())) {
                        queued.add(adjacentLocation.getOpposite());
                        shouldContinue = true;
                    }
                }
                if (canRemove)
                    iterator.remove();
            }
            if (!shouldContinue)
                break;
        }

//		drawDebugOutlines();

        if (!source.isPresent())
            source = sourceSupplier.get();
        if (!source.isPresent())
            return;

        keepPortableFluidInterfaceEngaged();

        if (targets.isEmpty())
            return;
        for (Pair<BlockFace, LazyOptional<IFluidHandler>> pair : targets) {
            if (pair.getSecond()
                    .isPresent() && world.getGameTime() % 40 != 0)
                continue;
            PipeConnection pipeConnection = get(pair.getFirst());
            PipeConnectionAccessor pipeConnectionAccessor = (PipeConnectionAccessor) pipeConnection;

            if (pipeConnection == null)
                continue;
            pipeConnectionAccessor.getSource().ifPresent(fs -> {
                if (fs.isEndpoint())
                    pair.setSecond(fs.provideHandler());
            });
        }

        int flowSpeed = transferSpeed;
        Map<IFluidHandler, Integer> accumulatedFill = new IdentityHashMap<>();

        for (boolean simulate : Iterate.trueAndFalse) {
            IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;

            IFluidHandler handler = source.orElse(null);
            if (handler == null)
                return;

            FluidStack transfer = FluidStack.EMPTY;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack contained = handler.getFluidInTank(i);
                if (contained.isEmpty())
                    continue;
                if (!contained.isFluidEqual(fluid))
                    continue;
                FluidStack toExtract = FluidHelper.copyStackWithAmount(contained, flowSpeed);
                transfer = handler.drain(toExtract, action);
            }

            if (transfer.isEmpty()) {
                FluidStack genericExtract = handler.drain(flowSpeed, action);
                if (!genericExtract.isEmpty() && genericExtract.isFluidEqual(fluid))
                    transfer = genericExtract;
            }

            if (transfer.isEmpty())
                return;
            if (simulate)
                flowSpeed = transfer.getAmount();

            List<Pair<BlockFace, LazyOptional<IFluidHandler>>> availableOutputs = new ArrayList<>(targets);

            while (!availableOutputs.isEmpty() && transfer.getAmount() > 0) {
                int dividedTransfer = transfer.getAmount() / availableOutputs.size();
                int remainder = transfer.getAmount() % availableOutputs.size();

                for (Iterator<Pair<BlockFace, LazyOptional<IFluidHandler>>> iterator =
                     availableOutputs.iterator(); iterator.hasNext();) {
                    Pair<BlockFace, LazyOptional<IFluidHandler>> pair = iterator.next();
                    int toTransfer = dividedTransfer;
                    if (remainder > 0) {
                        toTransfer++;
                        remainder--;
                    }

                    if (transfer.isEmpty())
                        break;
                    IFluidHandler targetHandler = pair.getSecond()
                            .orElse(null);
                    if (targetHandler == null) {
                        iterator.remove();
                        continue;
                    }

                    int simulatedTransfer = toTransfer;
                    if (simulate)
                        simulatedTransfer += accumulatedFill.getOrDefault(targetHandler, 0);

                    FluidStack divided = transfer.copy();
                    divided.setAmount(simulatedTransfer);
                    int fill = targetHandler.fill(divided, action);

                    if (simulate) {
                        accumulatedFill.put(targetHandler, Integer.valueOf(fill));
                        fill -= simulatedTransfer - toTransfer;
                    }

                    transfer.setAmount(transfer.getAmount() - fill);
                    if (fill < simulatedTransfer)
                        iterator.remove();
                }

            }

            flowSpeed -= transfer.getAmount();
            transfer = FluidStack.EMPTY;
        }
    }


}
