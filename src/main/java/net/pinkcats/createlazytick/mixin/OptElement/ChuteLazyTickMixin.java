package net.pinkcats.createlazytick.mixin.OptElement;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import net.pinkcats.createlazytick.helper.ScheduleTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ChuteBlockEntity.class, remap = false)
public class ChuteLazyTickMixin extends SmartBlockEntity implements IHaveGoggleInformation {

    @Shadow
    boolean canPickUpItems;

    @Shadow
    VersionedInventoryTrackerBehaviour invVersionTracker;

    @Shadow
    public boolean canDirectlyInsertCached() {return false;}

    @Shadow
    private boolean canDirectlyInsert() {return false;}

    @Shadow
    private void handleInputFromBelow() {}

    @Shadow
    public float getItemMotion() {return 0;}

    @Shadow
    private void spawnParticles(float itemMotion) {}

    @Shadow
    private void tickAirStreams(float itemSpeed) {}

    @Shadow
    ItemStack item;

    @Shadow
    private void handleInputFromAbove() {}

    @Shadow
    LerpedFloat itemPosition;

    @Shadow
    private boolean handleDownwardOutput(boolean simulate) {return false;}

    @Shadow
    private boolean handleUpwardOutput(boolean simulate) {return false;}

    public ChuteLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    int createLazyTick$chuteTick = 0;
    @Unique
    boolean createLazyTick$mistake = false;


    @Unique
    private void createLazyTick$LazyTickChute(boolean CanDownload){
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;
        if (control.createLazyTick$isDelayForced()) return;

        int currentLazyTickInterval = control.createLazyTick$getLazyTickInterval();
        if (level != null && !level.isClientSide) {
            // Current tick
            if (CanDownload) {
                control.createLazyTick$setLazyTickInterval(1);
                currentLazyTickInterval = 1;
                createLazyTick$mistake = false;
            } else {
                if (currentLazyTickInterval < Config.chute_delay_max) {
                    if (createLazyTick$mistake) {
                        int newDelayTick = Math.min(currentLazyTickInterval +
                                Math.max(1, currentLazyTickInterval /10), Config.chute_delay_max);
                        control.createLazyTick$setLazyTickInterval(newDelayTick);
                        currentLazyTickInterval = newDelayTick;
                    }
                    if (currentLazyTickInterval == 1) {
                        createLazyTick$mistake = true;
                    }
                }
            }
        }
    }

    @Unique
    private void createLazyTick$UserControl() {
        NetworkSyncHelper.createLazyTick$processUserControl((ISmartBlockEntityControl) this,Config.chute_delay_max);
    }

    @Unique
    private final ScheduleTicker UserControl_Schedule = new ScheduleTicker(5, this::createLazyTick$UserControl);


    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {

        if (!Config.enable_lazy_tick || !Config.enable_lazy_chute) {
            return;
        }

        super.tick();
        UserControl_Schedule.RandomTick();

        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;

        NetworkSyncHelper.createLazyTick$syncPacketData(control,
                this.level, this.worldPosition, control.createLazyTick$getLazyTickInterval(), Config.chute_delay_max);


        if (level != null && !level.isClientSide) canPickUpItems = canDirectlyInsert();

        boolean clientSide = level != null && level.isClientSide && !isVirtual();
        float itemMotion = getItemMotion();
        if (itemMotion != 0 && level != null && level.isClientSide)
            spawnParticles(itemMotion);
        tickAirStreams(itemMotion);

        if (item.isEmpty() && !clientSide) {
            if (itemMotion < 0)
                handleInputFromAbove();
            if (itemMotion > 0)
                handleInputFromBelow();
            ci.cancel();
            return;
        }

        int timeMultiplier = 1;

        if (level != null && !level.isClientSide) {
            // 获取当前实际的休眠间隔
            timeMultiplier = control.createLazyTick$getLazyTickInterval();
        }

        // 计算补偿位移
        float compensatedMotion = itemMotion * timeMultiplier;

        // 应用位移
        float nextOffset = itemPosition.getValue() + compensatedMotion;

        if (level != null && !level.isClientSide) {
            createLazyTick$chuteTick++;
            //System.out.println("chuteTick: "+createLazyTick$chuteTick+"|"+control.createLazyTick$getLazyTickInterval());
            if (createLazyTick$chuteTick < control.createLazyTick$getLazyTickInterval()) {
                ci.cancel();
                return;
            }
            createLazyTick$chuteTick = 0;
            //System.out.println("Run tick");
        }

        if (itemMotion < 0) {
            if (nextOffset < .5f) {

                boolean CanSimulateInput = handleDownwardOutput(true);

                createLazyTick$LazyTickChute(CanSimulateInput);


                if  (!CanSimulateInput) {
                    nextOffset = .5f;
                }

                else if (nextOffset < 0) {
                    boolean CanActualInput = handleDownwardOutput(clientSide);
                    createLazyTick$LazyTickChute(CanActualInput);



                    nextOffset = itemPosition.getValue();
                }
            }
        } else if (itemMotion > 0) {
            if (nextOffset > .5f) {
                if (!handleUpwardOutput(true))
                    nextOffset = .5f;
                else if (nextOffset > 1) {
                    boolean result = handleUpwardOutput(clientSide);
                    if (!result) {
                        CreateLazyTick.LOGGER.debug("Chute has modified Unpredicted. Location: {}", this.worldPosition);
                    }
                    nextOffset = itemPosition.getValue();
                }
            }
        }

        itemPosition.setValue(nextOffset);
        ci.cancel();
    }


    /**
     * @author PinkCats
     * @reason For Inject
     */
    @Overwrite
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen((d) -> canDirectlyInsertCached()));
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
        registerAwardables(behaviours, AllAdvancements.CHUTE);
    }
}
