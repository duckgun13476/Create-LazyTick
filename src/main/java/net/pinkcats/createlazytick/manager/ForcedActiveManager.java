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
 * 全局管理器：维护当前已加载的、被强制活跃的机器列表。
 * 性能安全：读写均为 O(1)，且仅在极少数被强制机器加载时触发。
 */
public class ForcedActiveManager {



    // Key: 维度 ResourceKey (例如 minecraft:overworld)
    // Value: 该维度下被强制活跃的 BlockPos 集合
    private static final Map<ResourceKey<Level>, Set<BlockPos>> forcedMachines = new ConcurrentHashMap<>();

    public static void register(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        // 使用 synchronizedSet 确保在多线程区块加载环境下安全
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
        // 返回副本以防止并发修改异常
        synchronized (set) {
            return new HashSet<>(set);
        }
    }

    public static void clear() {
        forcedMachines.clear();
    }
}