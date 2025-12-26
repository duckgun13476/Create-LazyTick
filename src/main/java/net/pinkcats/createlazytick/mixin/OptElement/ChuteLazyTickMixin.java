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

import net.pinkcats.createlazytick.Channel.ClientData;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

import static net.pinkcats.createlazytick.Channel.ClockSyncPacket.PacketCache;
import static net.pinkcats.createlazytick.item.LazyTickClockItem.StateDirection;


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
    int createLazyTick$CurrentDelayTick = 1;
    @Unique
    boolean createLazyTick$mistake = false;

    @Unique
    private void createLazyTick$LazyTickChute(boolean CanDownload){

        if (level != null && !level.isClientSide) {
            // Current tick
            //System.out.println(CurrentDelayTick);
            if (CanDownload) {
                createLazyTick$CurrentDelayTick = 1;
                createLazyTick$mistake = false;
            } else {
                if (createLazyTick$CurrentDelayTick < Config.chute_delay_max) {
                    if (createLazyTick$mistake) {
                        if (createLazyTick$CurrentDelayTick < 10) {
                            createLazyTick$CurrentDelayTick = createLazyTick$CurrentDelayTick + 1;
                        } else if (10 < createLazyTick$CurrentDelayTick && createLazyTick$CurrentDelayTick < 30) {
                            createLazyTick$CurrentDelayTick = createLazyTick$CurrentDelayTick + 2;
                        } else if (30 < createLazyTick$CurrentDelayTick && createLazyTick$CurrentDelayTick < 60) {
                            createLazyTick$CurrentDelayTick = createLazyTick$CurrentDelayTick + 3;
                        } else {
                            createLazyTick$CurrentDelayTick = createLazyTick$CurrentDelayTick + 5;
                        }
                        createLazyTick$CurrentDelayTick = createLazyTick$UserControl(createLazyTick$CurrentDelayTick);

                    }

                    if (createLazyTick$CurrentDelayTick == 1) {
                        createLazyTick$mistake = true;
                    }


                }
            }
        }
    }

    @Unique
    private int createLazyTick$UserControl(int CurrentDelayTick) {
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;
        byte CLTState = control.createLazyTick$ControlState();
        if (CLTState != 0){
            return Config.chute_delay_max / (StateDirection-1) * ((StateDirection-1) - CLTState);
        }
        System.out.println(CurrentDelayTick);
        return CurrentDelayTick;
    }


    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {

        if (!Config.enable_lazy_tick || !Config.enable_lazy_chute) {
            return;
        }


        super.tick();

        if (level != null && !level.isClientSide ) {
            if (!PacketCache.isEmpty()){
                for (ClientData data : PacketCache) {
                    if (data.getDimension() == level.dimension().hashCode()) {
                        if (Objects.equals(this.worldPosition, data.getPos())) {
                            ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;
                            control.lazytick$setSyncedTier(createLazyTick$CurrentDelayTick, Config.chute_delay_max);
                            PacketCache.remove(data);
                            break;
                        }
                    }
                }
            }




            //System.out.println("---");
            //System.out.println(control.CLT$getMaxTicks());
            //control.CLT$setMaxTicks(CurrentDelayTick);
            //System.out.println(control.CLT$getMaxTicks());
        }


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

        float nextOffset = itemPosition.getValue() + itemMotion;


        if (level != null && !level.isClientSide) {
            createLazyTick$chuteTick++;
            //System.out.println("chuteTick: "+chuteTick+"|"+CurrentDelayTick);
            if (createLazyTick$chuteTick < createLazyTick$CurrentDelayTick) {
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
