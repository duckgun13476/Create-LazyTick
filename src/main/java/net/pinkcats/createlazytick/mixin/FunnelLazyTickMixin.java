package net.pinkcats.createlazytick.mixin;

import net.pinkcats.createlazytick.Config;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
    private int extractionCooldown;

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
    public int getAmountToExtract() {return 0;};

    @Shadow
    public ItemHelper.ExtractionCountMode getModeToExtract() {return null;}


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
    private void createfastschematiccannon$startCooldown() {
        extractionCooldown = AllConfigs.server().logistics.defaultExtractionTimer.get() + ActuakMultiCount;
        //System.out.println("extraction cooldown: " + extractionCooldown);
    }

    @Shadow
    public void onTransfer(ItemStack stack) {}

    @Unique
    private int ActuakMultiCount = 0;



    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {
        flap.tickChaser();
        if (createfastschematiccannon$HasInterface){
            ActuakMultiCount = 0;

        }


        if(!createfastschematiccannon$HasInterface){
            if (extractionCooldown > 0) {
                extractionCooldown--;
                ci.cancel();
                return;
            }
            createfastschematiccannon$startCooldown();
        }



        super.tick();

        Funnel.Mode mode = determineCurrentMode();

        if (level.isClientSide) {
            ci.cancel();
            return;
        }


        // Redstone resets the extraction cooldown
        if (mode == Funnel.Mode.PAUSED)
            extractionCooldown = 0;
        if (mode == Funnel.Mode.TAKING_FROM_BELT) {
            ci.cancel();
            return;
        }

        if (createfastschematiccannon$HasInterface){
            if (extractionCooldown > 0) {
                extractionCooldown--;
                ci.cancel();
                return;
            }
        }
        BlockState blockState = getBlockState();
        BlockPos blockPos = getBlockPos();
        createfastschematiccannon$HasInterface = IsMovingInterface(blockPos,blockState);

        if (mode == Funnel.Mode.PUSHING_TO_BELT)
            activateExtractingBeltFunnel();
        if (mode == Funnel.Mode.EXTRACT)
            activateExtractor();

        ci.cancel();
    }

    @Unique
    private Direction createfastschematiccannon$targetDirection = null;


    @Unique
    private boolean createfastschematiccannon$HasInterface = false;

    private boolean IsMovingInterface(BlockPos blockPos,BlockState blockState) {
        if (createfastschematiccannon$targetDirection == null) {
            if (level != null) {
                try {

                    Direction[] allDirections = {Direction.UP, Direction.DOWN,
                            Direction.NORTH, Direction.SOUTH,
                            Direction.EAST, Direction.WEST};

                    for (Direction dir : allDirections) {
                        Block block = level.getBlockState(blockPos.relative(dir)).getBlock();
                       // System.out.println("Checking " + dir + ": " + block); // 调试用，可保留

                        // 找到目标方块：记录方向并返回true
                        if (

                                block.toString().equals("Block{create:portable_storage_interface}") ||
                                        block.toString().equals("Block{create:deployer}")




                        ) { // 替换成你的判断条件
                            createfastschematiccannon$targetDirection = dir;
                            return true;
                        }
                    }


                }
                catch(Exception e){
                    System.out.println(e);
                    return false;
                }

            }
        }
        else {
            if (level != null) {
                Block target = level.getBlockState(blockPos.relative(createfastschematiccannon$targetDirection)).getBlock();
                // 替换成你的判断条件
                return target.toString().equals("Block{create:portable_storage_interface}");
            }
        }

        return false;
    }



    @Inject(method = "activateExtractingBeltFunnel" ,at=@At("HEAD" ),cancellable = true,remap = false)
    private void activateExtractingBeltFunnel(CallbackInfo ci) {

        if (invVersionTracker.stillWaiting(invManipulation)) {
            ci.cancel();
            return;
        }
        BlockState blockState = getBlockState();

        //System.out.println();
        Direction facing = blockState.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
        DirectBeltInputBehaviour inputBehaviour =
                BlockEntityBehaviour.get(level, worldPosition.below(), DirectBeltInputBehaviour.TYPE);

        if (inputBehaviour == null) {
            ci.cancel();
            return;
        }
        if (!inputBehaviour.canInsertFromSide(facing)) {
            ci.cancel();
            return;
        }
        if (inputBehaviour.isOccupied(facing)) {
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
        //

        if (stack.isEmpty()) {

            //System.out.println("stack is empty");
            if (ActuakMultiCount < Config.funnel_delay_max) {
                ActuakMultiCount = ActuakMultiCount + 10;
            }
            createfastschematiccannon$startCooldown();
            if (deniedByInsertion.isFalse())
                invVersionTracker.awaitNewVersion(invManipulation.getInventory());
            return;
        }

        if (ActuakMultiCount >=10 ) {
            ActuakMultiCount  = ActuakMultiCount - 30;
        }

        flap(false);
        onTransfer(stack);
        inputBehaviour.handleInsertion(stack, facing, false);
        createfastschematiccannon$startCooldown();
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



