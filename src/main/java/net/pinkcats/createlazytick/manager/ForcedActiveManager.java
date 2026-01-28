package net.pinkcats.createlazytick.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author : *Fugit-5414*
 * <p>
 * Global class for save manipulated lazytick's machine
 * Thread secure
 */
public class ForcedActiveManager {

    private static final AtomicLong dataVersion = new AtomicLong(0);

    public static void register(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if(LazyTickSavedData.get(serverLevel).add(pos)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        if (level instanceof ServerLevel serverLevel) {
            if(LazyTickSavedData.get(serverLevel).remove(pos)) {
                dataVersion.incrementAndGet();
            }
        }
    }

    public static Set<BlockPos> getForcedPositions(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return LazyTickSavedData.get(serverLevel).getPositions();
        }
        return Collections.emptySet();
    }

    public static long getVersion() {
        return dataVersion.get();
    }
}