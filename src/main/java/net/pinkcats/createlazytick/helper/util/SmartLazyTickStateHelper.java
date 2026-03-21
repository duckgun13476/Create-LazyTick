package net.pinkcats.createlazytick.helper.util;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTier;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SmartLazyTickStateHelper {

    private static final Map<Key, State> STATES = new ConcurrentHashMap<>();

    private SmartLazyTickStateHelper() {}

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity != null
                && blockEntity.getLevel() != null
                && (blockEntity instanceof DepotBlockEntity
                || blockEntity instanceof FunnelBlockEntity
                || blockEntity instanceof ChuteBlockEntity
                || blockEntity instanceof ItemDrainBlockEntity);
    }

    public static ISmartBlockEntityControl control(BlockEntity blockEntity) {
        if (!supports(blockEntity)) {
            return null;
        }
        return new SmartControlAdapter(blockEntity, getOrCreateState(blockEntity));
    }

    public static void initialize(BlockEntity blockEntity) {
        if (supports(blockEntity) && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            LazyTickLogic.updateState(control(blockEntity), blockEntity);
        }
    }

    public static void invalidate(BlockEntity blockEntity) {
        if (!supports(blockEntity)) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        remove(level, blockEntity.getBlockPos());

        if (!level.isClientSide && level.isLoaded(blockEntity.getBlockPos())) {
            BlockState stateInLevel = level.getBlockState(blockEntity.getBlockPos());
            if (!stateInLevel.is(blockEntity.getBlockState().getBlock())) {
                ForcedActiveManager.unregister(level, blockEntity.getBlockPos());
            }
        }
    }

    public static void write(BlockEntity blockEntity, CompoundTag tag) {
        if (!supports(blockEntity)) {
            return;
        }

        State state = getOrCreateState(blockEntity);
        if (!state.ownerUUID.equals(Util.NIL_UUID)) {
            tag.putUUID("cltUUID", state.ownerUUID);
        }
        if (!state.ownerName.isEmpty()) {
            tag.putString("cltOwner", state.ownerName);
        }

        tag.putInt("cltCurrentInterval", state.currentInterval);
        if (state.syncedTier != LazyTickTier.ACTIVE) {
            tag.putInt("cltTier", state.syncedTier.ordinal());
        }
        if (state.extraData != 0) {
            tag.putInt("cltExtraData", state.extraData);
        }
        if (state.dynamicValue != 100) {
            tag.putInt("cltDynamic", state.dynamicValue);
        }
        if (state.forcedValue != -1) {
            tag.putInt("cltForced", state.forcedValue);
        }
    }

    public static void read(BlockEntity blockEntity, CompoundTag tag) {
        if (!supports(blockEntity)) {
            return;
        }

        State state = getOrCreateState(blockEntity);
        state.ownerUUID = tag.hasUUID("cltUUID") ? tag.getUUID("cltUUID") : Util.NIL_UUID;
        state.ownerName = tag.contains("cltOwner") ? tag.getString("cltOwner") : "";
        state.syncedTier = tag.contains("cltTier")
                ? LazyTickTier.values()[Math.max(0, Math.min(tag.getInt("cltTier"), LazyTickTier.values().length - 1))]
                : LazyTickTier.ACTIVE;
        state.extraData = tag.contains("cltExtraData") ? tag.getInt("cltExtraData") : 0;
        state.dynamicValue = tag.contains("cltDynamic") ? tag.getInt("cltDynamic") : 100;
        state.forcedValue = tag.contains("cltForced") ? tag.getInt("cltForced") : -1;
        state.currentInterval = Math.max(1, tag.contains("cltCurrentInterval") ? tag.getInt("cltCurrentInterval") : 1);

        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            LazyTickTooltipWhiteList white = LazyTickTooltipWhiteList.getByEntity(blockEntity);
            if (white != null) {
                state.syncedTier = LazyTickTier.fromTicks(state.currentInterval, white.getMaxTick());
            }
        }
    }

    public static void remove(Level level, BlockPos pos) {
        STATES.remove(new Key(level.dimension(), pos.immutable()));
    }

    private static State getOrCreateState(BlockEntity blockEntity) {
        return STATES.computeIfAbsent(new Key(blockEntity.getLevel().dimension(), blockEntity.getBlockPos().immutable()),
                ignored -> new State());
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final class State {
        private String ownerName = "";
        private UUID ownerUUID = Util.NIL_UUID;
        private LazyTickTier syncedTier = LazyTickTier.ACTIVE;
        private int currentInterval = 1;
        private int extraData = 0;
        private int dynamicValue = 100;
        private int forcedValue = -1;
    }

    private static final class SmartControlAdapter implements ISmartBlockEntityControl {
        private final BlockEntity blockEntity;
        private final State state;

        private SmartControlAdapter(BlockEntity blockEntity, State state) {
            this.blockEntity = blockEntity;
            this.state = state;
        }

        @Override
        public String createLazyTick$getOwnerName() {
            return state.ownerName;
        }

        @Override
        public void createLazyTick$setOwnerName(String value) {
            if (!Objects.equals(state.ownerName, value)) {
                state.ownerName = value;
                markDirty();
            }
        }

        @Override
        public UUID createLazyTick$getOwnerUUID() {
            return state.ownerUUID;
        }

        @Override
        public void createLazyTick$setOwnerUUID(UUID uuid) {
            if (!Objects.equals(state.ownerUUID, uuid)) {
                state.ownerUUID = uuid;
                markDirty();
            }
        }

        @Override
        public BlockPos CLT$getPos() {
            return blockEntity.getBlockPos();
        }

        @Override
        public ResourceKey<Level> CLT$getDimension() {
            return blockEntity.getLevel() != null ? blockEntity.getLevel().dimension() : null;
        }

        @Override
        public void lazytick$setSyncedTier(int currentTick, int maxTick) {
            state.syncedTier = LazyTickTier.fromTicks(currentTick, maxTick);
        }

        @Override
        public LazyTickTier lazytick$getSyncedTier() {
            return state.syncedTier;
        }

        @Override
        public boolean lazytick$isDefaultState() {
            return state.dynamicValue == 100 && state.forcedValue == -1;
        }

        @Override
        public int createLazyTick$getDynamicValue() {
            return state.dynamicValue;
        }

        @Override
        public void createLazyTick$setDynamicValue(int value) {
            if (state.dynamicValue != value) {
                state.dynamicValue = value;
                if (lazytick$isDefaultState()) {
                    state.ownerName = "";
                }
                markDirty();
            }
        }

        @Override
        public int createLazyTick$getForcedValue() {
            return state.forcedValue;
        }

        @Override
        public void createLazyTick$setForcedValue(int value) {
            if (state.forcedValue != value) {
                state.forcedValue = value;
                if (lazytick$isDefaultState()) {
                    state.ownerName = "";
                }
                markDirty();
            }
        }

        @Override
        public void createLazyTick$setDelayForced(boolean isForced) {
            if (!isForced) {
                state.forcedValue = -1;
            } else if (state.forcedValue == -1) {
                state.forcedValue = 1;
            }
            markDirty();
        }

        @Override
        public boolean createLazyTick$isDelayForced() {
            return state.forcedValue != -1;
        }

        @Override
        public void createLazyTick$setCurrentSuperTick(int tick) {
            int safeTick = Math.max(1, tick);
            if (state.currentInterval != safeTick) {
                state.currentInterval = safeTick;
                markDirty();
            }
        }

        @Override
        public int createLazyTick$getCurrentSuperTick() {
            return state.currentInterval;
        }

        @Override
        public void lazytick$setExtraData(int data) {
            if (state.extraData != data) {
                state.extraData = data;
                markDirty();
            }
        }

        @Override
        public int lazytick$getExtraData() {
            return state.extraData;
        }

        @Override
        public void createLazyTick$sendBlockUpdated() {
            Level level = blockEntity.getLevel();
            if (level != null) {
                BlockState blockState = blockEntity.getBlockState();
                level.sendBlockUpdated(blockEntity.getBlockPos(), blockState, blockState, 3);
            }
        }

        @Override
        public boolean CLT$IsController() {
            return false;
        }

        private void markDirty() {
            Level level = blockEntity.getLevel();
            if (level != null) {
                level.blockEntityChanged(blockEntity.getBlockPos());
            }
            blockEntity.setChanged();
        }
    }
}
