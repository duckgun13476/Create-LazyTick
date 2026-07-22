package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTier;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

@Mixin(value = DepotBlockEntity.class, remap = false)
public abstract class DepotControlProbeMixin extends BlockEntity implements ISmartBlockEntityControl {

    @Unique private String lazytick$ownerName = "";
    @Unique private UUID lazytick$ownerUUID = Util.NIL_UUID;
    @Unique private LazyTickTier lazytick$syncedTier = LazyTickTier.ACTIVE;
    @Unique private int createLazyTick$CurrentDelayTick = 1;
    @Unique private int lazytick$extraData = 0;
    @Unique private int lazyTick$dynamicValue = 100;
    @Unique private int lazyTick$forcedValue = -1;

    public DepotControlProbeMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "initialize", at = @At("RETURN"), remap = false)
    private void lazytick$onInit(CallbackInfo ci) {
        mes.info("[DepotControlProbeMixin][init] pos=" + this.worldPosition
                + ", side=" + (this.level != null && this.level.isClientSide ? "client" : "server"));
        if (this.level != null && !this.level.isClientSide()) {
            LazyTickLogic.updateState(this);
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void lazytick$onInvalidate(CallbackInfo ci) {
        if (level == null || level.isClientSide) return;
        if (!level.isLoaded(this.worldPosition)) return;
        BlockState stateInLevel = level.getBlockState(this.worldPosition);
        if (!stateInLevel.is(this.getBlockState().getBlock())) {
            ForcedActiveManager.unregister(level, this.worldPosition);
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void lazytick$writeNBT(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!this.lazytick$ownerUUID.equals(Util.NIL_UUID)) {
            tag.putUUID("cltUUID", this.lazytick$ownerUUID);
        }
        if (!this.lazytick$ownerName.isEmpty()) {
            tag.putString("cltOwner", this.lazytick$ownerName);
        }
        tag.putInt("cltCurrentInterval", this.createLazyTick$CurrentDelayTick);
        if (this.lazytick$syncedTier != LazyTickTier.ACTIVE) {
            tag.putInt("cltTier", this.lazytick$syncedTier.ordinal());
        }
        if (this.lazytick$extraData != 0) {
            tag.putInt("cltExtraData", this.lazytick$extraData);
        }
        if (this.lazyTick$dynamicValue != 100) {
            tag.putInt("cltDynamic", this.lazyTick$dynamicValue);
        }
        if (this.lazyTick$forcedValue != -1) {
            tag.putInt("cltForced", this.lazyTick$forcedValue);
        }
        mes.info("[DepotControlProbeMixin][write] clientPacket=" + clientPacket + ", keys=" + tag.getAllKeys());
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readNBT(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        this.lazytick$ownerUUID = tag.hasUUID("cltUUID") ? tag.getUUID("cltUUID") : Util.NIL_UUID;
        this.lazytick$ownerName = tag.contains("cltOwner") ? tag.getString("cltOwner") : "";
        this.lazytick$syncedTier = tag.contains("cltTier")
                ? LazyTickTier.values()[Math.max(0, Math.min(tag.getInt("cltTier"), LazyTickTier.values().length - 1))]
                : LazyTickTier.ACTIVE;
        this.lazytick$extraData = tag.contains("cltExtraData") ? tag.getInt("cltExtraData") : 0;
        this.lazyTick$dynamicValue = tag.contains("cltDynamic") ? tag.getInt("cltDynamic") : 100;
        this.lazyTick$forcedValue = tag.contains("cltForced") ? tag.getInt("cltForced") : -1;
        if (tag.contains("cltCurrentInterval")) {
            this.createLazyTick$CurrentDelayTick = tag.getInt("cltCurrentInterval");
        }
        mes.info("[DepotControlProbeMixin][read] clientPacket=" + clientPacket
                + ", keys=" + tag.getAllKeys()
                + ", dyn=" + this.lazyTick$dynamicValue
                + ", forced=" + this.lazyTick$forcedValue
                + ", interval=" + this.createLazyTick$CurrentDelayTick);
    }

    @Override
    public String createLazyTick$getOwnerName() {
        return this.lazytick$ownerName;
    }

    @Override
    public void createLazyTick$setOwnerName(String value) {
        if (!Objects.equals(this.lazytick$ownerName, value)) {
            this.lazytick$ownerName = value;
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition);
            this.setChanged();
        }
    }

    @Override
    public UUID createLazyTick$getOwnerUUID() {
        return this.lazytick$ownerUUID;
    }

    @Override
    public void createLazyTick$setOwnerUUID(UUID uuid) {
        if (!Objects.equals(this.lazytick$ownerUUID, uuid)) {
            this.lazytick$ownerUUID = uuid;
            if (this.level != null) this.level.blockEntityChanged(this.worldPosition);
            this.setChanged();
        }
    }

    @Override
    public BlockPos CLT$getPos() {
        return this.worldPosition;
    }

    @Override
    public ResourceKey<Level> CLT$getDimension() {
        return this.level != null ? this.level.dimension() : null;
    }

    @Override
    public void lazytick$setSyncedTier(int currentTick, int maxTick) {
        this.lazytick$syncedTier = LazyTickTier.fromTicks(currentTick, maxTick);
    }

    @Override
    public LazyTickTier lazytick$getSyncedTier() {
        return this.lazytick$syncedTier;
    }

    @Override
    public boolean lazytick$isDefaultState() {
        return this.lazyTick$dynamicValue == 100 && this.lazyTick$forcedValue == -1;
    }

    @Override
    public int createLazyTick$getDynamicValue() {
        return this.lazyTick$dynamicValue;
    }

    @Override
    public void createLazyTick$setDynamicValue(int value) {
        this.lazyTick$dynamicValue = value;
    }

    @Override
    public int createLazyTick$getForcedValue() {
        return this.lazyTick$forcedValue;
    }

    @Override
    public void createLazyTick$setForcedValue(int value) {
        this.lazyTick$forcedValue = value;
    }

    @Override
    public void createLazyTick$setDelayForced(boolean isForced) {
        this.lazyTick$forcedValue = isForced ? Math.max(1, this.lazyTick$forcedValue) : -1;
    }

    @Override
    public boolean createLazyTick$isDelayForced() {
        return this.lazyTick$forcedValue != -1;
    }

    @Override
    public void createLazyTick$setCurrentSuperTick(int tick) {
        this.createLazyTick$CurrentDelayTick = tick;
    }

    @Override
    public int createLazyTick$getCurrentSuperTick() {
        return this.createLazyTick$CurrentDelayTick;
    }

    @Override
    public void lazytick$setExtraData(int data) {
        this.lazytick$extraData = data;
    }

    @Override
    public int lazytick$getExtraData() {
        return this.lazytick$extraData;
    }

    @Override
    public void createLazyTick$sendBlockUpdated() {
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    public boolean CLT$IsController() {
        return false;
    }
}
