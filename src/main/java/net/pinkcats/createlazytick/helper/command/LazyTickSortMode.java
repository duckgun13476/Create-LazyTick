package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Comparator;
import java.util.Map;

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

        net.minecraft.world.phys.Vec3 playerVec = player.position();

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
    // 正常: 强制在前 (True > False)
    // 反序: 动态在前 (False > True)
    MODE("method", (e1, e2, source) -> {
        // Boolean.compare(true, false) = 1, 所以 e2 vs e1 是降序 (True 排前面)
        return Boolean.compare(e2.getValue().isForced(), e1.getValue().isForced());
    }),

    // 7. 数值排序
    // 正常: 从低到高 (1% -> 100%)
    // 反序: 从高到低
    VALUE("value", (e1, e2, source) ->
            Integer.compare(e1.getValue().getScrollValue(), e2.getValue().getScrollValue()));

    // --- 结构定义 ---

    private final String id;
    private final SortLogic logic;

    // 定义一个异常类型，用于 NEAREST 报错
    private static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(
            Component.literal("错误: 'nearest' (最近) 排序模式只能由玩家执行！")
    );

    LazyTickSortMode(String id, SortLogic logic) {
        this.id = id;
        this.logic = logic;
    }

    public String getId() {
        return id;
    }

    /**
     * 获取比较器
     * 逻辑/优先度：全局已加载在最前 > 指定逻辑排列 > 反序处理
     * @param source 命令源（获取玩家位置或Level）
     * @param reverse 反序
     * @return 构造比较器
     * @throws CommandSyntaxException 控制台使用 NEAREST 模式
     */
    public Comparator<Map.Entry<BlockPos, LazyTickStatCache>> getComparator(CommandSourceStack source, boolean reverse) throws CommandSyntaxException {
        // 特殊检查: 如果是 NEAREST 且执行者不是实体(比如控制台),直接抛错
        if (this == NEAREST && source.getEntity() == null) {
            throw ERROR_NOT_PLAYER.create();
        }

        return (e1, e2) -> {
            ServerLevel level = source.getLevel();
            boolean isLoaded1 = level.isLoaded(e1.getKey());
            boolean isLoaded2 = level.isLoaded(e2.getKey());

            // 1. 已加载区块优先,不受 reverse 参数影响
            // 如果 e1 加载了(true)而 e2 没加载(false)，返回 -1 (e1 排前)
            if (isLoaded1 != isLoaded2) {
                return isLoaded1 ? -1 : 1;
            }

            // 2. 排序
            int result = logic.compare(e1, e2, source);

            // 3. 反序,将结果取反
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