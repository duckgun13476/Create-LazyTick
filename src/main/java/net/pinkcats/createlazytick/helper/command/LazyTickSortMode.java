package net.pinkcats.createlazytick.helper.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public enum LazyTickSortMode {

    // 1. 默认排序 (坐标 XYZ)
    // 正常: 小 -> 大 (x1 < x2)
    // 反序: 大 -> 小
    DEFAULT("default", (e1, e2, source) -> {
        BlockPos p1 = e1.getKey();
        BlockPos p2 = e2.getKey();
        if (p1.getX() != p2.getX()) return Integer.compare(p1.getX(), p2.getX());
        if (p1.getY() != p2.getY()) return Integer.compare(p1.getY(), p2.getY());
        return Integer.compare(p1.getZ(), p2.getZ());
    }),

    // 2. 距离排序
    // 正常: 最近 -> 最远 (d1 < d2)
    // 反序: 最远 -> 最近
    NEAREST("nearest", (e1, e2, source) -> {
        // 获取实体
        Entity player = source.getEntity();
        if (player == null) {
            return 0; // 如果没有实体,不排序
        }

        Vec3 playerVec = player.position();

        // 3. 计算距离
        // distToCenterSqr : 方块中心(BlockPos + 0.5) 到 指定坐标(playerVec) 的距离平方 (a²+b²+c²)
        double dist1 = e1.getKey().distToCenterSqr(playerVec.x, playerVec.y, playerVec.z);
        double dist2 = e2.getKey().distToCenterSqr(playerVec.x, playerVec.y, playerVec.z);

        return Double.compare(dist1, dist2);
    }),

    // 3. 时间排序
    // 正常: 最新注册在前 (时间戳大 -> 小)
    // 反序: 最老注册在前 (时间戳小 -> 大)
    TIME("time", (e1, e2, source) ->
            Long.compare(e2.getValue().getRegisteredTime(), e1.getValue().getRegisteredTime())),

    // 4. 机器名排序
    // 正常: A -> Z
    // 反序: Z -> A
    NAME("name", (e1, e2, source) ->
            e1.getValue().getBlockName().compareTo(e2.getValue().getBlockName())),

    // 5. 玩家名排序
    // 正常: A -> Z
    // 反序: Z -> A
    PLAYER("player", (e1, e2, source) ->
            e1.getValue().getOwnerName().compareTo(e2.getValue().getOwnerName())),

    // 6. 模式排序
    // 正常: 强制在前 (True -> False)
    // 反序: 动态在前 (False -> True)
    MODE("method", (e1, e2, source) -> {
        // Boolean.compare(true, false) = 1, 所以 e2 vs e1 是降序 (True 排前面)
        return Boolean.compare(e2.getValue().isForced(), e1.getValue().isForced());
    }),

    // 7. 数值排序
    // 正常: 从低到高 (1% -> 100%)
    // 反序: 从高到低
    VALUE("value", (e1, e2, source) ->
            Integer.compare(e1.getValue().getScrollValue(), e2.getValue().getScrollValue())),

    // 8. 加载状态排序
    // 逻辑: 已加载在前，未加载在后(true -> false)
    // 效果: 正序优先显示已加载的机器
    // 具体逻辑在 getThreadSafeComparator 特殊处理,因为此处lambda无法获取到机器状态
    LOADED("loaded", (e1, e2, source) -> 0);

    // --- 结构定义 ---

    private final String id;
    private final SortLogic logic;

    LazyTickSortMode(String id, SortLogic logic) {
        this.id = id;
        this.logic = logic;
    }

    public String getId() {
        return id;
    }

    /**
     * (主逻辑与原有的getComparator基本一致,实现细节有变化)
     * @param loadedPositions 当前已加载的方块坐标集合(静态快照)
     * @param playerPos 玩家坐标(静态快照),控制台则为null
     * @param reverse 是否反序
     */
    public Comparator<Map.Entry<BlockPos, LazyTickStatCache>> getThreadSafeComparator(
            Set<BlockPos> loadedPositions, Vec3 playerPos, boolean reverse) {
        return (e1, e2) -> {
            int result;

            if (this == LOADED) {
                // 处理加载区域在前还是在后
                // 1. 使用传入的(已加载方块位置集合的静态快照) Set 处理区块加载逻辑
                boolean isLoaded1 = loadedPositions.contains(e1.getKey());
                boolean isLoaded2 = loadedPositions.contains(e2.getKey());
                result = Boolean.compare(isLoaded2, isLoaded1);
            } else if (this == NEAREST) {
                // 对NEAREST模式进行特殊处理,source可能非线程安全
                if (playerPos == null) {
                    result = 0; // 没有玩家坐标则视为相等
                } else {
                    // 否则使用传入的静态坐标计算距离
                    double d1 = e1.getKey().distToCenterSqr(playerPos.x, playerPos.y, playerPos.z);
                    double d2 = e2.getKey().distToCenterSqr(playerPos.x, playerPos.y, playerPos.z);
                    result = Double.compare(d1, d2);
                }
            } else {
                // 其他模式的source传null是安全的(没用到source)
                result = this.logic.compare(e1, e2, null);
            }

            // 反序处理
            return reverse ? -result : result;
        };
    }

    // 用于命令参数解析：通过字符串查找枚举
    public static LazyTickSortMode byName(String name) {
        for (LazyTickSortMode mode : values()) {
            if (mode.id.equalsIgnoreCase(name)) return mode;
        }
        return DEFAULT;
    }

    @FunctionalInterface
    interface SortLogic {
        int compare(Map.Entry<BlockPos, LazyTickStatCache> e1, Map.Entry<BlockPos, LazyTickStatCache> e2, CommandSourceStack source);
    }
}