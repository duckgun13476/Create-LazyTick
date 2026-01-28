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

import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickLogic;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
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
    private void createLazyTick$LazyTickChute(boolean CanDownload){
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;

        int currentLazyTickInterval = control.createLazyTick$getLazyTickInterval();
        if (level != null && !level.isClientSide) {
            // Current tick
            if (CanDownload) {
                LazyTickLogic.setIntervalSafe(control,1);
            } else {
                if (currentLazyTickInterval < ServerConfig.getChuteDelayMax()) {
                    int newInterval = LazyTickLogic.computeNextInterval(control, currentLazyTickInterval, ServerConfig.getChuteDelayMax());
                    LazyTickLogic.setIntervalSafe(control, newInterval);
                }
            }
        }
    }

    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {

        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyChute()) {
            return;
        }

        super.tick();

        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;

        NetworkSyncHelper.createLazyTick$syncPacketData(control,
                this.level, this.worldPosition, control.createLazyTick$getLazyTickInterval(), ServerConfig.getChuteDelayMax());


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
            if (item.isEmpty()) {
                // 空了要把计时器归零
                createLazyTick$chuteTick = 0;
            }
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
        // new ↓
        LazyTickScrollBehaviour.addTo(this, behaviours);
    }
}
