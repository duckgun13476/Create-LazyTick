package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickTier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SmartBlockEntity.class,remap = false)
public abstract class SmartBlockEntityControlMixin extends BlockEntity implements ISmartBlockEntityControl {

    @Unique private byte lazytick$forceDisabled = 0;
    @Unique private String lazytick$operatorName = "";
    @Unique private LazyTickTier lazytick$syncedTier = LazyTickTier.ACTIVE;
    @Unique private int createLazyTick$EntityMaxTick = 0;


    public SmartBlockEntityControlMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    // 1. 注入到 initialize()：这是 Create 初始化行为的地方
    //@Inject(method = "initialize", at = @At("RETURN"))
    //private void lazytick$onInit(CallbackInfo ci) {
    //    if (this.lazytick$forceDisabled == 0 && this.level != null) {
   //         ForcedActiveManager.register(this.level, this.worldPosition);
    //    }
    //}

    // 2. 注入到 invalidate()：这是 SmartBlockEntity.setRemoved() 必定会调用的清理方法
    //@Inject(method = "invalidate", at = @At("HEAD"))
   // private void lazytick$onInvalidate(CallbackInfo ci) {
     //   if (this.level != null) {
    //        ForcedActiveManager.unregister(this.level, this.worldPosition);
    //    }
    //}


    @Inject(method = "write", at = @At("RETURN"))
    private void lazytick$writeNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {

        tag.putByte("LazyTickForceDisabled", this.lazytick$forceDisabled);
        tag.putString("LazyTickOperator", this.lazytick$operatorName);

        if (this.lazytick$syncedTier != LazyTickTier.ACTIVE) {
            tag.putInt("LazyTickTier", this.lazytick$syncedTier.ordinal());
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("LazyTickForceDisabled")) {

            this.lazytick$forceDisabled = tag.getByte("LazyTickForceDisabled");
            this.lazytick$operatorName = tag.getString("LazyTickOperator");
        } else {
            this.lazytick$forceDisabled = 0;
            this.lazytick$operatorName = "";
        }

        if (tag.contains("LazyTickTier")) {
            int ordinal = tag.getInt("LazyTickTier");
            if (ordinal >= 0 && ordinal < LazyTickTier.values().length) {
                this.lazytick$syncedTier = LazyTickTier.values()[ordinal];
            }
        } else {
            this.lazytick$syncedTier = LazyTickTier.ACTIVE;
        }

    }


    @Override
    public LazyTickTier lazytick$getSyncedTier() { return this.lazytick$syncedTier; }

    @Override
    public void lazytick$setSyncedTier(int currentTick, int maxTick) {
        System.out.println("Update Sync Entity Data");
        LazyTickTier newTier = LazyTickTier.fromTicks(currentTick, maxTick);
        if (this.lazytick$syncedTier != newTier) {
            this.lazytick$syncedTier = newTier;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }
    }


    @Unique
    private void createLazyTick$sendBlockUpdated() {
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }




    // Interface
    @Override
    public byte createLazyTick$ControlState() { return this.lazytick$forceDisabled; }

    @Override
    public void createLazyTick$SetForceControl(byte value) {

        System.out.println("Change "+ value+" to "+this.lazytick$forceDisabled);

        if (this.lazytick$forceDisabled != value) {
            this.lazytick$forceDisabled = value;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }

    }

    @Override
    public String createLazyTick$getUserName() { return this.lazytick$operatorName; }

    @Override
    public void createLazyTick$setUserName(String value) {
        if (!Objects.equals(this.lazytick$operatorName, value)) {

            this.lazytick$operatorName = value;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }
    }

    @Override
    public int CLT$getMaxTicks() { return this.createLazyTick$EntityMaxTick; }

    @Override
    public void CLT$setMaxTicks(int value)  {
        if  (this.createLazyTick$EntityMaxTick != value) {
            this.createLazyTick$EntityMaxTick = value;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }
    }

    @Override
    public BlockPos CLT$getPos() {
        return this.worldPosition;
    }

    @Override
    public ResourceKey<Level> CLT$getDimension(){
        if (level != null) {
            return level.dimension();
        }
        return null;
    }


}