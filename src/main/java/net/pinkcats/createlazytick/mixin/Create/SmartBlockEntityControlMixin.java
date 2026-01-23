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
import net.pinkcats.createlazytick.helper.LazyTickLogic;
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

    @Unique private String lazytick$operatorName = "";
    @Unique private LazyTickTier lazytick$syncedTier = LazyTickTier.ACTIVE;
    @Unique private int createLazyTick$CurrentDelayTick = 1;
    @Unique private int lazytick$extraData = 0;
    @Unique private int lazyTick$dynamicValue = 100;
    @Unique private int lazyTick$forcedValue = -1;

    @Unique
    private boolean lazytick$isDefaultState() {
        return this.lazyTick$dynamicValue == 100 && this.lazyTick$forcedValue == -1;
    }

    public SmartBlockEntityControlMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Inject(method = "initialize", at = @At("RETURN"), remap = false)
    private void lazytick$onInit(CallbackInfo ci) {
        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(this);
        if (whiteItem == null) return;
        System.out.println("[LazyTick Init] Pos: " + this.worldPosition +
                " | Dyn: " + this.lazyTick$dynamicValue +
                " | Frc: " + this.lazyTick$forcedValue);
        if (this.level != null && !this.level.isClientSide()) {
            LazyTickLogic.updateState(this);
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

        if (this.lazytick$operatorName != null) {
            tag.putString("LazyTickOperator", this.lazytick$operatorName);
        }

        tag.putInt("LazyTickCurrentInterval", this.createLazyTick$CurrentDelayTick); // [关键新增]
        System.out.println("interval write");

        if (this.lazytick$syncedTier != LazyTickTier.ACTIVE) {
            tag.putInt("LazyTickTier", this.lazytick$syncedTier.ordinal());
            System.out.println("tier write");
        }
        if (this.lazytick$extraData != 0) {
            tag.putInt("LazyTickExtraData", this.lazytick$extraData);
            System.out.println("extradata write");
        }

        if (this.lazyTick$dynamicValue != 100) {
            tag.putInt("LazyTickDynamic", this.lazyTick$dynamicValue);
            System.out.println("dynamic write");
        }

        if (this.lazyTick$forcedValue != -1) {
            tag.putInt("LazyTickForced", this.lazyTick$forcedValue);
            System.out.println("force write");
        }
    }

    // disk -> Client
    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        /*if (tag.contains("LazyTickControlState")) {
            this.lazytick$operatorName = tag.getString("LazyTickOperator");
        } else {
            this.lazytick$controlState = 0;
            this.lazytick$operatorName = "";
        }*/
        System.out.println("data read");

        if (tag.contains("LazyTickOperator")) {
            this.lazytick$operatorName = tag.getString("LazyTickOperator");
        } else {
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

        // [关键新增] 读取数值
        if (tag.contains("LazyTickCurrentInterval")) {
            this.createLazyTick$CurrentDelayTick = tag.getInt("LazyTickCurrentInterval");
        }

        // [新增] 客户端收到数值后，自己推算颜色。
        // 这样服务端就不需要主动广播变色了！
        if (this.level != null && this.level.isClientSide) {
            LazyTickWhiteList white = LazyTickWhiteList.getByEntity(this);
            if (white != null) {
                this.lazytick$syncedTier = LazyTickTier.fromTicks(this.createLazyTick$CurrentDelayTick, white.getMaxTick());
            }
        }
    }


    @Override
    public LazyTickTier lazytick$getSyncedTier() { return this.lazytick$syncedTier; }

    @Override
    public void lazytick$setSyncedTier(int currentTick, int maxTick) {
        System.out.println("Update Sync Entity Data");
        this.lazytick$syncedTier = LazyTickTier.fromTicks(currentTick, maxTick);
        /*if (this.lazytick$syncedTier != newTier) {
            this.lazytick$syncedTier = newTier;
            this.setChanged();
            this.createLazyTick$sendBlockUpdated();
        }*/
    }

    @Override
    public int lazytick$getExtraData() {
        return this.lazytick$extraData;
    }

    @Override
    public void lazytick$setExtraData(int data) {
        if (this.lazytick$extraData != data) {
            this.lazytick$extraData = data;
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition);
            this.createLazyTick$sendBlockUpdated();
        }
    }


    @Override
    public void createLazyTick$sendBlockUpdated() {
        if (this.level != null) {
            BlockState state = this.getBlockState();
            if (!this.level.isClientSide) {
                long time = this.level.getGameTime();
                System.out.println("[Packet Check] 发送更新包 -> Pos: " + this.worldPosition + " | Time: " + time);
            }
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // Interface

    @Override
    public String createLazyTick$getUserName() { return this.lazytick$operatorName; }

    @Override
    public void createLazyTick$setUserName(String value) {
        if (!Objects.equals(this.lazytick$operatorName, value)) {
            this.lazytick$operatorName = value;
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition);
            this.setChanged();
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
        if (!isForced) {
            this.lazyTick$forcedValue = -1;
        } else {
            if (this.lazyTick$forcedValue == -1) {
                // 如果有人开启了强制模式却忘了设数值，默认给个 1 防止出 Bug
                this.lazyTick$forcedValue = 1;
            }
        }
        // 保存一下(数据一致性)
        this.setChanged();
    }

    @Override
    public boolean createLazyTick$isDelayForced() {
        return this.lazyTick$forcedValue != -1;
    }

    @Override
    public int createLazyTick$getDynamicValue() {
        return this.lazyTick$dynamicValue;
    }

    @Override
    public void createLazyTick$setDynamicValue(int value) {
        if (this.lazyTick$dynamicValue != value) {
            this.lazyTick$dynamicValue = value;
            if (lazytick$isDefaultState()) {
                this.lazytick$operatorName = "";
            }
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition); // save
            LazyTickLogic.updateState(this);
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
            if (lazytick$isDefaultState()) {
                this.lazytick$operatorName = "";
            }
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition);
            LazyTickLogic.updateState(this);
            this.createLazyTick$sendBlockUpdated();
        }
    }
}