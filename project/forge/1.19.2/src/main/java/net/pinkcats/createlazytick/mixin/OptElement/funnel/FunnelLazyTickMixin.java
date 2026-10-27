package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Funnel;
import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.*;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(value = FunnelBlockEntity.class, remap = false)
public abstract class FunnelLazyTickMixin extends SmartBlockEntity implements IHaveHoveringInformation {

    @Shadow
    LerpedFloat flap;

    @Shadow
    private InvManipulationBehaviour invManipulation;

    @Shadow
    protected abstract ItemStack handleDirectBeltInput(TransportedItemStack stack, Direction side, boolean simulate);

    @Shadow
    protected abstract boolean supportsAmountOnFilter();

    @Shadow
    private FilteringBehaviour filtering;

    @Shadow
    protected abstract boolean supportsFiltering();

    @Shadow
    protected abstract boolean supportsDirectBeltInput(Direction side);

    @Shadow
    private VersionedInventoryTrackerBehaviour invVersionTracker;


    public FunnelLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract void flap(boolean inward);

    @Shadow
    protected abstract void activateExtractingBeltFunnel();


    @Shadow
    protected abstract void activateExtractor();


    @Shadow
    public abstract int getAmountToExtract();

    @Shadow
    public abstract ItemHelper.ExtractionCountMode getModeToExtract();

    @Shadow
    public abstract void onTransfer(ItemStack stack);

    @Unique
    private static boolean createLazyTick$hasWarned = false;

    @Unique
    private String createLazyTick$buildIncompatibleSkipMessage() {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(this.getBlockState().getBlock());
        String dimension = this.level != null ? this.level.dimension().location().toString() : "unknown";
        return "[FunnelLazyTickMixin] Incompatible block entity detected; CLT will skip lazy tick handling and fall back to vanilla logic. "
                + "class=" + this.getClass().getName()
                + ", blockId=" + (blockId != null ? blockId : "unknown")
                + ", pos=" + this.worldPosition
                + ", dimension=" + dimension;
    }

    @Unique
    public Funnel.Mode determineCurrentMode() {
        BlockState state = getBlockState();
        if (!FunnelBlock.isFunnel(state))
            return Funnel.Mode.INVALID;
        if (state.getOptionalValue(BlockStateProperties.POWERED)
                .orElse(false))
            return Funnel.Mode.PAUSED;
        if (state.getBlock() instanceof BeltFunnelBlock) {
            BeltFunnelBlock.Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
            if (shape == BeltFunnelBlock.Shape.PULLING)
                return Funnel.Mode.TAKING_FROM_BELT;
            if (shape == BeltFunnelBlock.Shape.PUSHING)
                return Funnel.Mode.PUSHING_TO_BELT;

            BeltBlockEntity belt = BeltHelper.getSegmentBE(level, worldPosition.below());
            if (belt != null)
                return belt.getMovementFacing() == state.getValue(BeltFunnelBlock.HORIZONTAL_FACING)
                        ? Funnel.Mode.PUSHING_TO_BELT
                        : Funnel.Mode.TAKING_FROM_BELT;
            return Funnel.Mode.INVALID;
        }
        if (state.getBlock() instanceof FunnelBlock)
            return state.getValue(FunnelBlock.EXTRACTING) ? Funnel.Mode.EXTRACT : Funnel.Mode.COLLECT;

        return Funnel.Mode.INVALID;
    }


    @Unique
    private void createLazyTick$applyBackoff(ISmartBlockEntityControl control) {
        int currentLazyTickInterval = control.createLazyTick$getCurrentSuperTick();
        int newLazyTickInterval = LazyTickLogic.computeNextInterval(
                control, currentLazyTickInterval, ServerConfig.getFunnelDelayMax()
        );
        // mes.error(newLazyTickInterval);
        if (newLazyTickInterval != currentLazyTickInterval) {
            LazyTickLogic.setIntervalSafe(control, newLazyTickInterval);
        }
    }

    @Unique
    private void createLazyTick$FunnelBackOff(ISmartBlockEntityControl control){
        createLazyTick$applyBackoff(control);
        createLazyTick$applyBackoff(control);
    }

    @Unique
    private void createLazyTick$resetDelayTick(ISmartBlockEntityControl control) {
        int defaultTick = AllConfigs.server().logistics.defaultExtractionTimer.get();
        CLT$FunnelDelayTick = 0;
        LazyTickLogic.setIntervalSafe(control, defaultTick);
    }



    @Unique
    private int CLT$FunnelDelayTick = 0;

    @Unique
    private boolean createLazyTick$wasPaused = false;



    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {

        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFunnel()) {
            return;
        }
        if (level == null) {
            ci.cancel();
            return;
        }

        if (!(this instanceof ISmartBlockEntityControl control)) {
            if (!createLazyTick$hasWarned) {
                createLazyTick$hasWarned = true;
                mes.error(createLazyTick$buildIncompatibleSkipMessage());
            }
            return;
        }


        super.tick();
        flap.tickChaser();

        if (level.isClientSide) {
            ci.cancel();
            return;
        }

        Funnel.Mode mode = determineCurrentMode();

        // Redstone resets the extraction cooldown
        if (mode == Funnel.Mode.PAUSED) {
            createLazyTick$resetDelayTick(control);
            createLazyTick$wasPaused = true;
            ci.cancel();
            return;
        }
        boolean justUnpaused = ServerConfig.getEnableFunnelOverclock()
            && createLazyTick$wasPaused
                && (mode == Funnel.Mode.EXTRACT || mode == Funnel.Mode.PUSHING_TO_BELT);
        createLazyTick$wasPaused = false;
        if (justUnpaused) {
            if (mode == Funnel.Mode.EXTRACT)
                activateExtractor();
            else
                activateExtractingBeltFunnel();
            ci.cancel();
            return;
        }
        if (mode == Funnel.Mode.TAKING_FROM_BELT) {
            ci.cancel();
            return;
        }

        // Read the actual cached extraction target, not unrelated neighboring blocks.
        // Deployer overflow blocks its next operation until drained: retain vanilla
        // extraction cadence even when the selected lazy interval is longer.
        Object inventory = invManipulation.getInventory();
        CLT$FunnelDelayTick++;
        if ((mode == Funnel.Mode.EXTRACT || mode == Funnel.Mode.PUSHING_TO_BELT)
                && inventory instanceof net.pinkcats.createlazytick.bridge.Create.IPortableInterfaceLease.Source source) {
            int remaining = control.createLazyTick$getCurrentSuperTick() - CLT$FunnelDelayTick;
            source.createLazyTick$getLeaseOwner().createLazyTick$awaitFunnel(
                    (FunnelBlockEntity) (Object) this, inventory, remaining);
        }
        if (!(inventory instanceof com.simibubi.create.content.kinetics.deployer.DeployerItemHandler)) {
            if (CLT$FunnelDelayTick < control.createLazyTick$getCurrentSuperTick()) {
                ci.cancel();
                return;
            }
        } else {
            if (CLT$FunnelDelayTick < AllConfigs.server().logistics.defaultExtractionTimer.get()) {
                ci.cancel();
                return;
            }
        }
        CLT$FunnelDelayTick = 0;


        if (mode == Funnel.Mode.PUSHING_TO_BELT) {
            //mes.warn("if (mode == Funnel.Mode.PUSHING_TO_BELT) {");
            activateExtractingBeltFunnel();

        }

        if (mode == Funnel.Mode.EXTRACT) {
            //mes.warn("if (mode == Funnel.Mode.EXTRACT) {");
            activateExtractor();
        }


        ci.cancel();
    }

    @Inject(method = "activateExtractingBeltFunnel" ,at=@At("HEAD" ),cancellable = true,remap = false)
    private void clt$activateExtractingBeltFunnel(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFunnel()) {
            return;
        }

        if (!(this instanceof ISmartBlockEntityControl control)) {
            //mes.error("BlockEntity is not a SmartBlockEntityControl!");
            return;}

        if (invVersionTracker.stillWaiting(invManipulation)) {
            //mes.blue("if (invVersionTracker.stillWaiting(invManipulation)) {");
            ci.cancel();
            return;
        }
        BlockState blockState = getBlockState();

        //System.out.println();
        Direction facing = blockState.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
        DirectBeltInputBehaviour inputBehaviour =
                BlockEntityBehaviour.get(level, worldPosition.below(), DirectBeltInputBehaviour.TYPE);

        if (inputBehaviour == null) {
           // mes.blue("if (inputBehaviour == null) {");
            ci.cancel();
            return;
        }
        if (!inputBehaviour.canInsertFromSide(facing)) {
           // mes.blue("   if (!inputBehaviour.canInsertFromSide(facing)) {");
            ci.cancel();
            return;
        }
        if (inputBehaviour.isOccupied(facing)) {
            createLazyTick$FunnelBackOff(control);
            ci.cancel();
            return;
        }

        int amountToExtract = getAmountToExtract();
        ItemHelper.ExtractionCountMode mode = getModeToExtract();
        MutableBoolean deniedByInsertion = new MutableBoolean(false);

        // delay from this
        ItemStack stack = invManipulation.extract(mode, amountToExtract, s -> {
            ItemStack handleInsertion = inputBehaviour.handleInsertion(s, facing, true);
            if (handleInsertion.isEmpty()) {
                //System.out.println("Extracting fail!");
                ci.cancel();
                return true;
            }
            deniedByInsertion.setTrue();
            //System.out.println("Extracting success!");
            ci.cancel();
            return false;
        });

        if (stack.isEmpty()) {

            //mes.blue("if (stack.isEmpty()) {");
            createLazyTick$FunnelBackOff(control);
            if (deniedByInsertion.isFalse())
                invVersionTracker.awaitNewVersion(invManipulation.getInventory());
            ci.cancel();
            return;
        }

        flap(false);
        onTransfer(stack);
        inputBehaviour.handleInsertion(stack, facing, false);

        createLazyTick$resetDelayTick(control);
       // mes.blue("end");
        ci.cancel();
    }





    /**
     * @author PinkCats
     * @reason For Inject
     */
    @Overwrite
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        invManipulation =
                new InvManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, AbstractFunnelBlock.getFunnelFacing(s)
                        .getOpposite()));
        behaviours.add(invManipulation);

        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));

        filtering = new FilteringBehaviour(this, new FunnelFilterSlotPositioning());
        filtering.showCountWhen(this::supportsAmountOnFilter);
        filtering.onlyActiveWhen(this::supportsFiltering);
        filtering.withCallback($ -> invVersionTracker.reset());
        behaviours.add(filtering);

        behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput)
                .setInsertionHandler(this::handleDirectBeltInput));
        registerAwardables(behaviours, AllAdvancements.FUNNEL);
    }








}



