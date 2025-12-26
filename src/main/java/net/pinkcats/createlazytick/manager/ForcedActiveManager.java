package net.pinkcats.createlazytick.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author : *Fugit-5414*
 * <p>
 * Global class for save manipulated lazytick's machine
 * Thread secure
 */
public class ForcedActiveManager {


    // Key: Dimension ResourceLocation
    // Value: BlockPos<>
    private static final Map<ResourceKey<Level>, Set<BlockPos>> forcedMachines = new ConcurrentHashMap<>();

    public static void register(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        // Sync safe
        forcedMachines.computeIfAbsent(level.dimension(), k -> Collections.synchronizedSet(new HashSet<>()))
                .add(pos);
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        ResourceKey<Level> dim = level.dimension();
        if (forcedMachines.containsKey(dim)) {
            forcedMachines.get(dim).remove(pos);
        }
    }

    public static Set<BlockPos> getForcedPositions(Level level) {
        if (level == null) return Collections.emptySet();
        Set<BlockPos> set = forcedMachines.get(level.dimension());
        if (set == null) return Collections.emptySet();

        // return back for thread issue
        synchronized (set) {
            return new HashSet<>(set);
        }
    }

    public static void clear() {
        forcedMachines.clear();
    }
}