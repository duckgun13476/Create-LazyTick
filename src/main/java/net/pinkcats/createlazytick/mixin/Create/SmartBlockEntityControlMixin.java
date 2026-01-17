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
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTier;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = SmartBlockEntity.class,remap = false)
public abstract class SmartBlockEntityControlMixin extends BlockEntity implements ISmartBlockEntityControl {

    @Unique private byte lazytick$controlState = 0;
    @Unique private String lazytick$operatorName = "";
    @Unique private LazyTickTier lazytick$syncedTier = LazyTickTier.ACTIVE;
    @Unique private int createLazyTick$CurrentDelayTick = 1;
    @Unique private boolean createLazyTick$isDelayForced = false;
    @Unique private int lazytick$extraData = 0;
    @Unique private int lazyTick$dynamicValue = 100;
    @Unique private int lazyTick$forcedValue = -1;

    public SmartBlockEntityControlMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Inject(method = "initialize", at = @At("RETURN"), remap = false)
    private void lazytick$onInit(CallbackInfo ci) {
        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(this);
        if (whiteItem == null) return;
        System.out.println("init PARA" + this.lazytick$controlState);
        if (this.lazytick$controlState != 0 && this.level != null) {
            ForcedActiveManager.register(this.level, this.worldPosition);
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void lazytick$onInvalidate(CallbackInfo ci) {
        if (this.level != null) {
            ForcedActiveManager.unregister(this.level, this.worldPosition);
        }
    }

    // Server -> disk
    @Inject(method = "write", at = @At("RETURN"))
    private void lazytick$writeNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(this);
        if (whiteItem == null) return;

        tag.putByte("LazyTickControlState", this.lazytick$controlState);
        tag.putString("LazyTickOperator", this.lazytick$operatorName);

        if (this.lazytick$syncedTier != LazyTickTier.ACTIVE) {
            tag.putInt("LazyTickTier", this.lazytick$syncedTier.ordinal());
        }
        if (this.lazytick$extraData != 0) {
            tag.putInt("LazyTickExtraData", this.lazytick$extraData);
        }

        if (this.lazyTick$dynamicValue != 100) {
            tag.putInt("LazyTickDynamic", this.lazyTick$dynamicValue);
        }

        if (this.lazyTick$forcedValue != -1) {
            tag.putInt("LazyTickForced", this.lazyTick$forcedValue);
        }
    }

    // disk -> Client
    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("LazyTickControlState")) {
            this.lazytick$controlState = tag.getByte("LazyTickControlState");
            this.lazytick$operatorName = tag.getString("LazyTickOperator");
        } else {
            this.lazytick$controlState = 0;
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


        if (tag.contains("LazyTickExtraData")) {
            this.lazytick$extraData = tag.getInt("LazyTickExtraData");
        } else {
            this.lazytick$extraData = 0;
        }

        if (tag.contains("LazyTickDynamic")) {
            this.lazyTick$dynamicValue = tag.getInt("LazyTickDynamic");
        } else {
            this.lazyTick$dynamicValue = 100;
        }

        if (tag.contains("LazyTickForced")) {
            this.lazyTick$forcedValue = tag.getInt("LazyTickForced");
        } else {
            this.lazyTick$forcedValue = -1;
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

    @Override
    public int lazytick$getExtraData() {
        return this.lazytick$extraData;
    }

    @Override
    public void lazytick$setExtraData(int data) {
        if (this.lazytick$extraData != data) {
            this.lazytick$extraData = data;
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
    public byte createLazyTick$ControlState() { return this.lazytick$controlState; }

    @Override
    public void createLazyTick$SetForceControl(byte value) {

        System.out.println("Change "+ value+" to "+this.lazytick$controlState);

        if (this.lazytick$controlState != value) {
            this.lazytick$controlState = value;
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

    @Override
    public void createLazyTick$setLazyTickInterval(int tick) {
        this.createLazyTick$CurrentDelayTick = tick;
    }

    @Override
    public int createLazyTick$getLazyTickInterval() {
        return this.createLazyTick$CurrentDelayTick;
    }

    @Override
    public void createLazyTick$setDelayForced(boolean isForced) {
        this.createLazyTick$isDelayForced = isForced;
    }

    @Override
    public boolean createLazyTick$isDelayForced() {
        return this.createLazyTick$isDelayForced;
    }

    @Override
    public int createLazyTick$getDynamicValue() {
        return this.lazyTick$dynamicValue;
    }

    @Override
    public void createLazyTick$setDynamicValue(int value) {
        if (this.lazyTick$dynamicValue != value) {
            this.lazyTick$dynamicValue = value;
            this.setChanged(); // save
            this.createLazyTick$sendBlockUpdated(); // sync to client (UI render)
        }
    }

    @Override
    public int createLazyTick$getForcedValue() {
        return this.lazyTick$forcedValue;
    }

    @Override
    public void createLazyTick$setForcedValue(int value) {
        if (this.lazyTick$forcedValue != value) {
            this.lazyTick$forcedValue = value;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }
    }
}