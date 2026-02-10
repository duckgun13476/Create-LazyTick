package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.CreateLazyTick;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.simibubi.create.AllBlocks.DEPLOYER;
import static com.simibubi.create.AllBlocks.PORTABLE_STORAGE_INTERFACE;

@Mixin(value = FunnelBlockEntity.class, remap = false)
public class FunnelLazyTickMixin extends SmartBlockEntity implements IHaveHoveringInformation {

    @Shadow
    LerpedFloat flap;


    @Shadow
    private InvManipulationBehaviour invManipulation;

    @Shadow
    private ItemStack handleDirectBeltInput(TransportedItemStack stack, Direction side, boolean simulate) {
        return null;
    }

    @Shadow
    private boolean supportsAmountOnFilter() {return false;}

    @Shadow
    private FilteringBehaviour filtering;

    @Shadow
    private boolean supportsFiltering() {return false;}

    @Shadow
    private boolean supportsDirectBeltInput(Direction side) {return false;}

    @Shadow
    private VersionedInventoryTrackerBehaviour invVersionTracker;


    public FunnelLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public void flap(boolean inward) {}

    @Shadow
    private void activateExtractingBeltFunnel() {}


    @Shadow
    private void activateExtractor() {}


    @Shadow
    public int getAmountToExtract() {return 0;}

    @Shadow
    public ItemHelper.ExtractionCountMode getModeToExtract() {return null;}

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
        mes.error(newLazyTickInterval);
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
        createLazyTick$FunnelDelayTick = defaultTick;
        LazyTickLogic.setIntervalSafe(control, defaultTick);
    }

    @Shadow
    private int extractionCooldown;

    @Unique
    private void createlazytick$startCooldown() {
        extractionCooldown = AllConfigs.server().logistics.defaultExtractionTimer.get() + createLazyTick$FunnelDelayTick;
        //System.out.println("extraction cooldown: " + extractionCooldown);
    }

    @Shadow
    public void onTransfer(ItemStack stack) {}

    @Unique
    private int createLazyTick$FunnelDelayTick = 0;



    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {

        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFunnel()) {
            return;
        }
        if (level == null) {
            return;
        }

        if (!(this instanceof ISmartBlockEntityControl control)) {
            mes.error("BlockEntity is not a SmartBlockEntityControl!");
            return;}

        flap.tickChaser();

        // for interface
        if (createlazytick$HasInterface){
            createLazyTick$FunnelDelayTick = (control.createLazyTick$getCurrentSuperTick() - 10) ;
        }

        createLazyTick$FunnelDelayTick++;
        if (createLazyTick$FunnelDelayTick < control.createLazyTick$getCurrentSuperTick()) {
            ci.cancel();
            return;}
        createLazyTick$FunnelDelayTick = 0;

        super.tick();
        Funnel.Mode mode = determineCurrentMode();

        if (level.isClientSide) {
            ci.cancel();
            return;
        }


        // Redstone resets the extraction cooldown
        if (mode == Funnel.Mode.PAUSED)
            createLazyTick$resetDelayTick(control);
        if (mode == Funnel.Mode.TAKING_FROM_BELT) {
            ci.cancel();
            return;
        }


        BlockState blockState = getBlockState();
        BlockPos blockPos = getBlockPos();
        createlazytick$HasInterface = createLazyTick$IsMovingInterface(blockPos,blockState);

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

    @Unique
    private Direction createlazytick$targetDirection = null;


    @Unique
    private boolean createlazytick$HasInterface = false;

    @Inject(method = "activateExtractingBeltFunnel" ,at=@At("HEAD" ),cancellable = true,remap = false)
    private void activateExtractingBeltFunnel(CallbackInfo ci) {
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

        AtomicInteger extract_time = new AtomicInteger();
        // delay from this
        ItemStack stack = invManipulation.extract(mode, amountToExtract, s -> {
            extract_time.getAndIncrement();
            //System.out.println("Extracting belt funnel"+extract_time);

            ItemStack handleInsertion = inputBehaviour.handleInsertion(s, facing, true);
            if (handleInsertion.isEmpty()) {
                //System.out.println("Extracting fail!");
                return true;
            }
            deniedByInsertion.setTrue();
            //System.out.println("Extracting success!");
            return false;
        });

        if (stack.isEmpty()) {

            //mes.blue("if (stack.isEmpty()) {");
            createLazyTick$FunnelBackOff(control);
            if (deniedByInsertion.isFalse())
                invVersionTracker.awaitNewVersion(invManipulation.getInventory());
            return;
        }

        flap(false);
        onTransfer(stack);
        inputBehaviour.handleInsertion(stack, facing, false);

        createLazyTick$resetDelayTick(control);
       // mes.blue("end");
        ci.cancel();
    }





    //Tool func
    @Unique
    private boolean createLazyTick$IsMovingInterface(BlockPos blockPos, BlockState blockState) {

        if (createlazytick$targetDirection == null) {
            if (level != null) {
                try {

                    Direction[] allDirections = {Direction.UP, Direction.DOWN,
                            Direction.NORTH, Direction.SOUTH,
                            Direction.EAST, Direction.WEST};

                    for (Direction dir : allDirections) {
                        Block block = level.getBlockState(blockPos.relative(dir)).getBlock();

                        // 找到目标方块：记录方向并返回true
                        if (block == PORTABLE_STORAGE_INTERFACE.get() || block == DEPLOYER.get()) {
                            createlazytick$targetDirection = dir;
                            return true;
                        }
                    }
                }
                catch(Exception e){
                    CreateLazyTick.LOGGER.error(e.getMessage());
                    return false;
                }

            }
        }
        else {
            if (level != null) {
                Block target = level.getBlockState(blockPos.relative(createlazytick$targetDirection)).getBlock();
                return target == PORTABLE_STORAGE_INTERFACE.get() || target == DEPLOYER.get();
            }
        }

        return false;
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



