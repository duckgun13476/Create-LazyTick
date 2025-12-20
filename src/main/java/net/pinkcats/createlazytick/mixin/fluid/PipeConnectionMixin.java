package net.pinkcats.createlazytick.mixin.fluid;

import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.PipeConnection;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(value = PipeConnection.class,remap = false)
public class PipeConnectionMixin {

    @Shadow
    Optional<FluidNetwork> network; // not serialized

    @Shadow
    Optional<FlowSource> source;

    @Shadow
    Couple<Float> pressure;

    @Shadow
    public boolean determineSource(Level world, BlockPos pos) {return false;}

    @Shadow
    public boolean hasFlow() {
        return false;
    }

    @Shadow
    public boolean hasPressure() {return false;}

    @Shadow
    public float comparePressure(){return 0;}

    @Shadow
    private boolean tryStartingNewFlow(boolean inbound, FluidStack providedFluid) {return false;}

    @Shadow
    public boolean hasNetwork() {
        return false;
    }

    @Shadow
    public Direction side;

    @Shadow
    Optional<PipeConnection.Flow> flow;




    @Inject(method = "tryStartingNewFlow" ,at=@At("HEAD" ),cancellable = true,remap = false)
    private void tryStartingNewFlow(boolean inbound, FluidStack providedFluid, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("NewFlow"+ providedFluid.getAmount());

        //if (providedFluid.isEmpty()) {
        //    cir.setReturnValue(false);
         //   return;
        //}
        //PipeConnection.Flow flow = new PipeConnection.Flow(inbound, providedFluid);
        //this.flow = Optional.of(flow);
        //cir.setReturnValue(true);
    }



    @Inject(method = "manageFlows" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void manageFlows(Level world, BlockPos pos, FluidStack internalFluid, Predicate<FluidStack> extractionPredicate, CallbackInfoReturnable<Boolean> cir) {

        System.out.println("manageFlows");


        // Only keep network if still valid
        Optional<FluidNetwork> retainedNetwork = network;
        network = Optional.empty();

        // chunk border
        if (!source.isPresent() && !determineSource(world, pos)) {
            cir.setReturnValue(false);
            return;
        }
        FlowSource flowSource = source.get();

        if (!hasFlow()) {
            if (!hasPressure()) {
                System.out.println("if (!hasPressure()) {");
                cir.setReturnValue(false);
                return;
            }

            // Try starting a new flow
            boolean prioritizeInbound = comparePressure() < 0;
            for (boolean trueFalse : Iterate.trueAndFalse) {
                boolean inbound = prioritizeInbound == trueFalse;
                if (pressure.get(inbound) == 0)
                    continue;
                if (tryStartingNewFlow(inbound, inbound ? flowSource.provideFluid(extractionPredicate) : internalFluid)) {
                    System.out.println("tryStartingNewFlow");
                    cir.setReturnValue(true);
                    return;
                }
            }
            System.out.println("if (!hasFlow()) {");
            cir.setReturnValue(false);
            return;
        }

        // Manage existing flow
        PipeConnection.Flow flow = this.flow.get();
        FluidStack provided = flow.inbound ? flowSource.provideFluid(extractionPredicate) : internalFluid;
        if (!hasPressure() || provided.isEmpty() || !provided.isFluidEqual(flow.fluid)) {
            System.out.println("Manage existing flow");
            this.flow = Optional.empty();
            cir.setReturnValue(true);
            return;
        }

        // Overwrite existing flow
        if (flow.inbound != comparePressure() < 0) {
            boolean inbound = !flow.inbound;
            if (inbound && !provided.isEmpty() || !inbound && !internalFluid.isEmpty()) {
                FluidPropagator.resetAffectedFluidNetworks(world, pos, side);
                tryStartingNewFlow(inbound, inbound ? flowSource.provideFluid(extractionPredicate) : internalFluid);
                System.out.println("Overwrite existing flow");
                cir.setReturnValue(true);
                return;
            }
        }

        flowSource.whileFlowPresent(world, flow.inbound);

        if (!flowSource.isEndpoint()) {
            System.out.println("isEndpoint");
            cir.setReturnValue(false);
            return;
        }
        if (!flow.inbound) {
            System.out.println("(!flow.inbound)");
            cir.setReturnValue(false);
            return;
        }

        // Layer III
        network = retainedNetwork;
        if (!hasNetwork())
            network = Optional.of(new FluidNetwork(world, new BlockFace(pos, side), flowSource::provideHandler));
        network.get()
                .tick();
        System.out.println("end");
        cir.setReturnValue(false);
    }


}
