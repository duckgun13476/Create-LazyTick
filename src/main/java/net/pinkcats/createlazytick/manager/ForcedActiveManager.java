package net.pinkcats.createlazytick.manager;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : *Fugit-5414*
 * <p>
 * Global class for save manipulated lazytick's machine
 * Thread secure
 */
public class ForcedActiveManager {

    private static final AtomicLong dataVersion = new AtomicLong(0);

    public static void register(Level level, BlockPos pos, String blockName, UUID ownerUUID,
                                String ownerName, int scrollValue, boolean isForced) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            LazyTickSavedStat savedData = LazyTickSavedStat.get(serverLevel);
            LazyTickStatCache existingInfo = savedData.getMachinesMap().get(pos);

            long timeToRecord = System.currentTimeMillis();
            if (existingInfo != null) {
                boolean isSameOwner = existingInfo.getOwnerUUID().equals(ownerUUID);
                boolean isSameValue = existingInfo.getScrollValue() == scrollValue;
                boolean isSameMode = existingInfo.isForced() == isForced;

                if (isSameOwner && isSameValue && isSameMode) {
                    timeToRecord = existingInfo.getRegisteredTime();
                }
            }

            LazyTickStatCache info = new LazyTickStatCache(
                    blockName,
                    ownerUUID,
                    ownerName,
                    timeToRecord,
                    scrollValue,
                    isForced
            );

            if (LazyTickSavedStat.get(serverLevel).add(pos, info)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if (LazyTickSavedStat.get(serverLevel).remove(pos)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    public static int executeBatchReset(ServerLevel level, List<BlockPos> targets) {
        if (targets.isEmpty()) return 0;

        int validResetCount = 0;
        int dirtyDataCount = 0;
        boolean dataChanged = false;
        LazyTickSavedStat data = LazyTickSavedStat.get(level);

        for (BlockPos pos : targets) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(pos);
            ISmartBlockEntityControl control = resolveControl(be);
            if (control != null && !control.lazytick$isDefaultState()) {
                dataChanged = true;
                LazyTickLogic.switchMode(control, false, 100);
                syncUpdatedState(control, be);
                validResetCount++;

                if (be instanceof SmartBlockEntity sbe) {
                    LazyTickScrollBehaviour behaviour = LazyTickLogic.getBehaviour(sbe, LazyTickScrollBehaviour.class);
                    if (behaviour != null) {
                        behaviour.setValue(100);
                    }
                }
            }

            if (data.remove(pos)) {
                dataChanged = true;
                dirtyDataCount++;
            }
        }

        if (dataChanged) {
            dataVersion.incrementAndGet();
        }
        CreateLazyTick.LOGGER.debug("Cleared {} invalid lazytick data entries", dirtyDataCount);

        return validResetCount;
    }

    public static Map<BlockPos, LazyTickStatCache> getForcedMachines(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return LazyTickSavedStat.get(serverLevel).getMachinesMap();
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("resource")
    public static boolean canPlayerActivate(BlockEntity blockEntity, Player player) {
        if (player.level().isClientSide) return true;

        ServerLevel level = (ServerLevel) player.level();
        UUID playerUUID = player.getUUID();
        LazyTickSavedLimitList limitData = LazyTickSavedLimitList.get(level);
        int limit = limitData.getLimit(playerUUID);

        if (limit == -1) return true;

        if (limit == 0) {
            player.displayClientMessage(Component.translatable("createlazytick.command.ban")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        ISmartBlockEntityControl control = resolveControl(blockEntity);
        if (control != null && control.createLazyTick$getOwnerUUID().equals(playerUUID)) {
            String currentName = player.getName().getString();
            if (!control.createLazyTick$getOwnerName().equals(currentName)) {
                control.createLazyTick$setOwnerName(currentName);
            }
            return true;
        }

        int currentUsage = ForcedActiveManager.getPlayerUsageCount(level, playerUUID);
        if (currentUsage >= limit) {
            player.displayClientMessage(Component.translatable(
                            "createlazytick.message.adjust_limit_reached",
                            currentUsage,
                            limit
                    )
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        return true;
    }

    public static int getPlayerUsageCount(Level level, UUID playerUUID) {
        if (!(level instanceof ServerLevel serverLevel)) return 0;

        MinecraftServer server = serverLevel.getServer();
        int count = 0;

        for (ServerLevel currentLevel : server.getAllLevels()) {
            Map<BlockPos, LazyTickStatCache> map = LazyTickSavedStat.get(currentLevel).getMachinesMap();
            for (LazyTickStatCache info : map.values()) {
                if (info.getOwnerUUID().equals(playerUUID)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static long getVersion() {
        return dataVersion.get();
    }

    private static ISmartBlockEntityControl resolveControl(BlockEntity blockEntity) {
        if (blockEntity instanceof ISmartBlockEntityControl control) {
            return control;
        }
        return SmartLazyTickStateHelper.control(blockEntity);
    }

    private static void syncUpdatedState(ISmartBlockEntityControl control, BlockEntity blockEntity) {
        if (SmartLazyTickStateHelper.supports(blockEntity)) {
            LazyTickLogic.updateState(control, blockEntity);
            control.createLazyTick$sendBlockUpdated();
        } else {
            LazyTickLogic.updateState(control);
        }
    }
}
