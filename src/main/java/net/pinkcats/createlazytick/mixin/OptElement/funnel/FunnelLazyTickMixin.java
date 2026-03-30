package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Funnel;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
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
import net.createmod.catnip.math.BlockFace;
import net.createmod.catnip.animation.LerpedFloat;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.simibubi.create.AllBlocks.DEPLOYER;
import static com.simibubi.create.AllBlocks.PORTABLE_STORAGE_INTERFACE;

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
            ci.cancel();
            return;
        }
        if (mode == Funnel.Mode.TAKING_FROM_BELT) {
            ci.cancel();
            return;
        }

        BlockState blockState = getBlockState();
        BlockPos blockPos = getBlockPos();
        CLT$HasInterface = createLazyTick$IsMovingInterface(blockPos,blockState);


        // for interface
        CLT$FunnelDelayTick++;
        if (!CLT$HasInterface){
            if (CLT$FunnelDelayTick < control.createLazyTick$getCurrentSuperTick()) {
                ci.cancel();
                return;}

        } else {
            if (CLT$FunnelDelayTick < AllConfigs.server().logistics.defaultExtractionTimer.get()) {
                ci.cancel();
                return;}
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

    @Unique
    private Direction createlazytick$targetDirection = null;


    @Unique
    private boolean CLT$HasInterface = false;

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

        AtomicInteger extract_time = new AtomicInteger();
        // delay from this
        ItemStack stack = invManipulation.extract(mode, amountToExtract, s -> {
            extract_time.getAndIncrement();
            //System.out.println("Extracting belt funnel"+extract_time);

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





    //Tool func

    //paradox Code (interesting func)
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



