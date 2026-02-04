package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandHelper {
    private static final int PAGE_SIZE = 15;

    // 不可跨纬度列表
    // FOR LIST(可异步)
    // 不用缓存了,异步线程池交给你了()
    public static int executeList(CommandContext<CommandSourceStack> context, int page, LazyTickSortMode sortMode, boolean isReverse) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 1. 从管理器获取原始数据
        Map<BlockPos, LazyTickStatCache> forcedMachines = ForcedActiveManager.getForcedMachines(level);
        if (forcedMachines.isEmpty()) {
            source.sendSystemMessage(Component.literal("当前名单中没有非默认配置的机器").withStyle(ChatFormatting.GREEN));
            return 0;
        }

        // 创建快照(必须主线程)
        CommandHelper.SortContext snapshot = CommandHelper.createSnapshot(source, forcedMachines.keySet());

        // 2. 转为List准备排序
        List<Map.Entry<BlockPos, LazyTickStatCache>> sortedEntries = new ArrayList<>(forcedMachines.entrySet());

        // 3. 使用LazyTickSortMode进行排序(异步主要针对此处代码块)
        try {
            Comparator<Map.Entry<BlockPos, LazyTickStatCache>> comparator =
                    sortMode.getThreadSafeComparator(snapshot.getLoadedPositions(), snapshot.getPlayerPos(), isReverse);
            sortedEntries.sort(comparator);
        } catch (Exception e1) {
            source.sendFailure(Component.literal("执行排序时发生内部错误,请联系管理员查看控制台"));
            CreateLazyTick.LOGGER.error(e1.getMessage());
            // 排序失败则降级为默认排序再次尝试
            try {
                sortedEntries.sort(LazyTickSortMode.DEFAULT.getThreadSafeComparator(snapshot.getLoadedPositions(),
                        snapshot.getPlayerPos(), false));
            } catch (Exception e2) {
                CreateLazyTick.LOGGER.error("回退默认方法排序失败:\n{}", e2.getMessage());
            }
        }

        // 必须返回主线程执行
        CommandHelper.renderAllAndCleanData(source, level, sortedEntries, page, sortMode, isReverse);
        // 结束
        return 1;
    }

    // 不可跨纬度重置
    // FOR RESET(可异步)
    public static int executeReset(CommandContext<CommandSourceStack> context, Component description, Predicate<Map.Entry<BlockPos, LazyTickStatCache>> filter) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        Map<BlockPos, LazyTickStatCache> forcedMachines = ForcedActiveManager.getForcedMachines(level);
        if (forcedMachines.isEmpty()) {
            source.sendFailure(Component.literal("没有任何记录可供重置"));
            return 0;
        }

        // 创建快照(主线程)
        CommandHelper.SortContext snapshot = CommandHelper.createSnapshot(source, forcedMachines.keySet());

        // 进行筛选(异步主要针对此处代码块)
        List<BlockPos> candidates = new ArrayList<>();
        for (Map.Entry<BlockPos, LazyTickStatCache> entry : forcedMachines.entrySet()) {
            // 满足谓词(由 Handler 提供)
            if (filter.test(entry)) {
                // 且必须在已加载区块内(快照判断)
                if (snapshot.getLoadedPositions().contains(entry.getKey())) {
                    candidates.add(entry.getKey());
                }
            }
        }

        // 执行逻辑(必须回主线程)
        int count = ForcedActiveManager.executeBatchReset(level, candidates);

        if (count > 0) {
            source.sendSuccess(() -> Component.literal("已在加载区域内重置 " + count + " 个匹配 ")
                    .append(description).append(" 的机器").append("\n")
                    .append(Component.literal("(未加载区域保持不变)").withStyle(ChatFormatting.GRAY)), true);
        } else {
            source.sendFailure(Component.literal("在当前已加载区域未找到匹配 ").append(description).append(" 的记录"));
        }
        return 1;
    }

    // For clt list/reset TIME==============================
    // 每对圆括号为一组,严格限制一个关键词为两组(数字+单位 "3d")
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);
    public static long parseDuration(String input) throws NumberFormatException {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalMs = 0;
        boolean foundAny = false;

        // 从头开始,每找到一次符合的就进行一次计算,然后从最近一次找到符合的位置开始继续寻找
        while (matcher.find()) {
            foundAny = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            totalMs += switch (unit) {
                case "d" -> value * 24 * 60 * 60 * 1000L;
                case "h" -> value * 60 * 60 * 1000L;
                case "m" -> value * 60 * 1000L;
                case "s" -> value * 1000L;
                default -> 0;
            };
        }

        if (!foundAny) {
            throw new NumberFormatException("无效的时间格式: " + input + " (示例: 3d; 12h; 30m; 3d8h6m30s)");
        }

        String leftOver = matcher.replaceAll("");
        if (!leftOver.isBlank()) {
            throw new NumberFormatException("时间包含非法字符: " + leftOver);
        }

        return totalMs;
    }

    // 静态类(快照)  For NEAREST=============================
    public static class SortContext {
        private final Vec3 playerPos;
        private final Set<BlockPos> loadedPositions;

        public SortContext(Vec3 playerPos, Set<BlockPos> loadedPositions) {
            this.playerPos = playerPos;
            this.loadedPositions = loadedPositions;
        }

        public Vec3 getPlayerPos() { return this.playerPos; }
        public Set<BlockPos> getLoadedPositions() { return this.loadedPositions; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SortContext that = (SortContext) o;
            return Objects.equals(playerPos, that.playerPos) &&
                    Objects.equals(loadedPositions, that.loadedPositions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerPos, loadedPositions);
        }
    }

    // For clt list/reset NEAREST(生成相关位置快照),与SortContext相关
    public static SortContext createSnapshot(CommandSourceStack source, Set<BlockPos> targets) {
        ServerLevel level = source.getLevel();

        // 1. 玩家坐标
        Vec3 playerPos = (source.getEntity() != null) ? source.getPosition() : null;

        // 2. 当前元件所有加载中位置的集合(快照)
        Set<BlockPos> loadedPositionsSnapshot = new HashSet<>();

        // 缓存优化,如果区块一致,不再get
        LongOpenHashSet loadedChunkCache = new LongOpenHashSet();
        LongOpenHashSet unloadedChunkCache = new LongOpenHashSet();

        for (BlockPos pos : targets) {
            long chunkId = ChunkPos.asLong(pos);

            if (loadedChunkCache.contains(chunkId)) {
                loadedPositionsSnapshot.add(pos);
                continue;
            }
            if (unloadedChunkCache.contains(chunkId)) {
                continue;
            }

            if (level.hasChunk(ChunkPos.getX(chunkId), ChunkPos.getZ(chunkId))) {
                loadedChunkCache.add(chunkId);
                loadedPositionsSnapshot.add(pos);
            } else {
                unloadedChunkCache.add(chunkId);
            }
        }

        // 返回排序上下文
        return new SortContext(playerPos, loadedPositionsSnapshot);
    }

    //For clt list===================================
    public static void renderAllAndCleanData(
            CommandSourceStack source, ServerLevel level, List<Map.Entry<BlockPos, LazyTickStatCache>> sortedEntries,
            int page, LazyTickSortMode sortMode, boolean isReverse
    ) {

        // 4. 分页计算
        int totalMachines = sortedEntries.size();
        int totalPages = (int) Math.ceil((double) totalMachines / PAGE_SIZE);

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalMachines);

        // 5. 制作聊天栏标题
        LazyTickListRenderer.renderHeader(source, page, totalPages, totalMachines, sortMode);

        // 6. 循环逐行渲染条目 + 清理无效数据
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<BlockPos, LazyTickStatCache> entry = sortedEntries.get(i);
            BlockPos pos = entry.getKey();

            // 只有区块已加载时, 才去检查元件是否还在
            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);

                // BE不存在/不是ISBEControl指定的元件/处于默认状态则清理
                if (!(be instanceof ISmartBlockEntityControl control) || control.lazytick$isDefaultState()) {
                    ForcedActiveManager.unregister(level, pos);

                    CreateLazyTick.LOGGER.debug("Cleared invalid lazytick data entries:{}",  pos.toShortString());
                    // 跳过本次渲染，不显示在列表里
                    // (注意:会导致当前页显示少一行,但无伤大雅(能跑就行))
                    continue;
                }
            }
            // 制作单行信息条目
            LazyTickListRenderer.renderItem(source, i + 1, entry, level.isLoaded(pos));
        }
        // 7. 制作翻页按钮
        LazyTickListRenderer.renderNavBar(source, page, totalPages, sortMode, isReverse);
    }

    // for SuggestionProvider======================================
    public static class DimensionCache {
        private Set<String> machineNames = new HashSet<>();
        private Set<String> machineOwners = new HashSet<>();
        private long lastUpdateTime = 0;

        public Set<String> getMachineNames() {return machineNames; }

        public Set<String> getMachineOwners() { return machineOwners; }

        public long getLastUpdateTime() { return lastUpdateTime; }
    }

    public static final Map<ResourceKey<Level>, DimensionCache> dimensionCaches = new ConcurrentHashMap<>();
    public static final long CACHE_TIMEOUT = 60000; //ms

    public static DimensionCache getDimensionMachineStatCache(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        DimensionCache cache = dimensionCaches.get(dimension);
        long now = System.currentTimeMillis();

        if (cache == null || now - cache.lastUpdateTime > CACHE_TIMEOUT) {
            cache = new DimensionCache();

            Collection<LazyTickStatCache> machines = ForcedActiveManager.getForcedMachines(level).values();

            // 获取当前维度的机器数据
            cache.machineNames = machines.stream()
                    .map(LazyTickStatCache::getBlockName)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toSet());

            cache.machineOwners = machines.stream()
                    .map(LazyTickStatCache::getOwnerName)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toSet());

            cache.lastUpdateTime = now;
            dimensionCaches.put(dimension, cache);
        }

        return cache;
    }
}
