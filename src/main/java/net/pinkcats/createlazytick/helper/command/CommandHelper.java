package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandHelper {
    private static final int PAGE_SIZE = 15;

    // 排序封装类 (模式 + 各自的独立反序标记)
    public static class SortCriterion {
        public final LazyTickSortMode mode;
        public final boolean isReverse;

        public SortCriterion(LazyTickSortMode mode, boolean isReverse) {
            this.mode = mode;
            this.isReverse = isReverse;
        }
    }

    // 不可跨纬度列表
    // FOR LIST(可异步)
    // 不用缓存了,异步线程池交给你了()
    public static int executeList(CommandContext<CommandSourceStack> context, int page, List<SortCriterion> criteria,
                                  boolean globalReverse, Predicate<Map.Entry<BlockPos, LazyTickStatCache>> filter,
                                  String rawSortStr, String rawFilterStr) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 1. 从管理器获取原始数据
        Map<BlockPos, LazyTickStatCache> forcedMachines = ForcedActiveManager.getForcedMachines(level);
        if (forcedMachines.isEmpty()) {
            source.sendSystemMessage(Component.literal("当前名单中没有非默认配置的机器").withStyle(ChatFormatting.GREEN));
            return 0;
        }

        // 创建区块是否加载以及执行位置上下文(主线程)
        CommandHelper.SortContext sortContext = CommandHelper.createSortContext(source, forcedMachines.keySet());

        // 创建机器数据快照(主线程)
        List<Map.Entry<BlockPos, LazyTickStatCache>> rawDataSnapshot = new ArrayList<>(forcedMachines.entrySet());

        // 2. 先筛选,筛选完成的转为List准备排序,如果没有符合条件的直接return(异步主要针对此处和第三步代码块)
        List<Map.Entry<BlockPos, LazyTickStatCache>> filteredEntries = new ArrayList<>();
        for (Map.Entry<BlockPos, LazyTickStatCache> entry : rawDataSnapshot) {
            if (filter.test(entry)) {
                filteredEntries.add(entry);
            }
        }

        if (filteredEntries.isEmpty()) {
            source.sendFailure(Component.literal("没有符合筛选条件的记录"));
            return 0;
        }

        // 3. 使用SortCriterion进行符合排序
        try {
            Comparator<Map.Entry<BlockPos, LazyTickStatCache>> finalComparator = null;
            for (SortCriterion criterion : criteria) {
                // 有效反序 = (单项反序 异或 全局反序)
                boolean effectiveReverse = (criterion.isReverse != globalReverse);

                Comparator<Map.Entry<BlockPos, LazyTickStatCache>> modeComparator =
                        criterion.mode.getThreadSafeComparator(sortContext.getLoadedPositions(), sortContext.getPlayerPos(), effectiveReverse);

                if (finalComparator == null) {
                    finalComparator = modeComparator;
                } else {
                    finalComparator = finalComparator.thenComparing(modeComparator); // 链式调用
                }
            }
            if (finalComparator != null) {
                filteredEntries.sort(finalComparator);
            }
        } catch (Exception e1) {
            source.sendFailure(Component.literal("执行排序时发生内部错误,请联系管理员查看控制台"));
            CreateLazyTick.LOGGER.error("List sort error: {}",e1.getMessage());
            // 排序失败则降级为默认排序再次尝试
            try {
                filteredEntries.sort(LazyTickSortMode.DEFAULT.getThreadSafeComparator(sortContext.getLoadedPositions(),
                        sortContext.getPlayerPos(), false));
            } catch (Exception e2) {
                CreateLazyTick.LOGGER.error("回退默认方法排序失败:\n{}", e2.getMessage());
            }
        }

        // 必须返回主线程执行
        CommandHelper.renderAllAndCleanData(source, level, filteredEntries, page, rawSortStr, globalReverse, rawFilterStr);
        // 结束
        return 1;
    }

    // for LIST
    public static List<CommandHelper.SortCriterion> parseSortString(String input) throws CommandSyntaxException {
        if (input == null || input.isBlank()) {
            return Collections.singletonList(new CommandHelper.SortCriterion(LazyTickSortMode.DEFAULT, false));
        }
        // 剥离大括号和引号
        String trimmed = FilterParser.stripBracesAndQuotes(input);

        String[] parts = trimmed.split("[,\\s]+");
        List<CommandHelper.SortCriterion> list = new ArrayList<>();

        for (String part : parts) {
            String cleanPart = part.trim();
            if (cleanPart.isEmpty()) continue;

            boolean isReverse = false;

            // 检测 "!" 前缀 (局部反序)
            if (cleanPart.startsWith("!")) {
                isReverse = true;
                cleanPart = cleanPart.substring(1).trim();
            }

            // 查找对应模式
            LazyTickSortMode mode = LazyTickSortMode.byName(cleanPart);

            if (mode == null) {
                throw new SimpleCommandExceptionType(
                        Component.literal("未知的排序模式: [")
                                .append(Component.literal(cleanPart).withStyle(ChatFormatting.UNDERLINE))
                                .append("] (可用: default, time, name, player, method, value, nearest, loaded)")
                ).create();
            }

            list.add(new CommandHelper.SortCriterion(mode, isReverse));
        }

        // 如果解析结果为空,返回含有一个默认值元素的列表
        if (list.isEmpty()) {
            list.add(new CommandHelper.SortCriterion(LazyTickSortMode.DEFAULT, false));
        }

        return list;
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

        // 创建区块是否加载以及执行位置上下文(主线程)
        CommandHelper.SortContext sortContext = CommandHelper.createSortContext(source, forcedMachines.keySet());

        // 进行筛选(异步主要针对此处代码块)
        List<BlockPos> candidates = new ArrayList<>();
        for (Map.Entry<BlockPos, LazyTickStatCache> entry : forcedMachines.entrySet()) {
            // 满足谓词(由 Handler 提供)
            if (filter.test(entry)) {
                // 且必须在已加载区块内(快照判断)
                if (sortContext.getLoadedPositions().contains(entry.getKey())) {
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

    // 复用 List Complex,无视分页,导出全量数据
    public static int executeDump(CommandContext<CommandSourceStack> context, List<SortCriterion> criteria,
                                  boolean globalReverse, Predicate<Map.Entry<BlockPos, LazyTickStatCache>> filter,
                                  String rawSortStr, String rawFilterStr) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 1: 在主线程创造数据快照(必须在主线程执行)
        // 从管理器获取原始数据
        Map<BlockPos, LazyTickStatCache> forcedMachines = ForcedActiveManager.getForcedMachines(level);
        if (forcedMachines.isEmpty()) {
            source.sendFailure(Component.literal("没有任何数据可供导出"));
            return 0;
        }

        // 创建位置和区块加载状态上下文快照
        CommandHelper.SortContext sortContext = CommandHelper.createSortContext(source, forcedMachines.keySet());

        // 创建数据列表快照
        List<Map.Entry<BlockPos, LazyTickStatCache>> rawDataSnapshot = new ArrayList<>(forcedMachines.entrySet());

        // 准备文件路径  // 需要确认是放在config里合适还是单开dump文件夹合适,修改此处请同事修改Line 333的输出信息
        Path serverRoot = source.getServer().getServerDirectory().toPath();
        Path dumpDir = serverRoot.resolve("dumps").resolve("createlazytick");

        // ----------------------------------------从这里往下到方法末尾都可以异步
        // 2: 筛选与排序
        // 以下内容线程安全

        // 执行筛选 (Filter)
        List<Map.Entry<BlockPos, LazyTickStatCache>> resultList = new ArrayList<>();
        for (Map.Entry<BlockPos, LazyTickStatCache> entry : rawDataSnapshot) {
            if (filter.test(entry)) {
                resultList.add(entry);
            }
        }

        if (resultList.isEmpty()) {
            source.sendFailure(Component.literal("没有符合筛选条件的记录,导出取消"));
            return 0;
        }

        // 执行排序 (Sort)
        try {
            Comparator<Map.Entry<BlockPos, LazyTickStatCache>> finalComparator = null;
            for (SortCriterion criterion : criteria) {
                // 计算实际反序状态 (局部 vs 全局)
                boolean effectiveReverse = (criterion.isReverse != globalReverse);

                // 使用快照数据获取比较器
                Comparator<Map.Entry<BlockPos, LazyTickStatCache>> modeComparator =
                        criterion.mode.getThreadSafeComparator(sortContext.getLoadedPositions(), sortContext.getPlayerPos(), effectiveReverse);

                if (finalComparator == null) {
                    finalComparator = modeComparator;
                } else {
                    finalComparator = finalComparator.thenComparing(modeComparator);
                }
            }
            if (finalComparator != null) {
                resultList.sort(finalComparator);
            }
        } catch (Exception e1) {
            source.sendFailure(Component.literal("排序时发生错误,将使用默认顺序导出"));
            CreateLazyTick.LOGGER.error("Dump sort error: ", e1);
            try {
                resultList.sort(LazyTickSortMode.DEFAULT.getThreadSafeComparator(sortContext.getLoadedPositions(),
                        sortContext.getPlayerPos(), false));
            } catch (Exception e2) {
                CreateLazyTick.LOGGER.error("回退默认方法排序失败:\n{}", e2.getMessage());
            }
        }

        // 3: IO 操作
        try {
            if (!Files.exists(dumpDir)) {
                Files.createDirectories(dumpDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = "Clt_Data_dump_" + timestamp + ".txt";
            Path filePath = dumpDir.resolve(fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                // --- 写入文件头 ---
                writer.write("=== Create Lazy Tick Data Dump ==="); writer.newLine();
                writer.write("Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); writer.newLine();
                writer.write("Filter Chain: " + (rawFilterStr.isBlank() ? "[ALL]" : rawFilterStr)); writer.newLine();
                writer.write("Sort Chain: " + rawSortStr + " (Global Reverse: " + globalReverse + ")"); writer.newLine();
                writer.write("Total Records: " + resultList.size()); writer.newLine();
                writer.write("----------------------------------------------------------------------------------"); writer.newLine();

                // --- 写入列名 ---
                // 格式对齐: Location(25) | Name(30) | Owner(16) | Mode(8) | Val(6) | Loaded(7) | Time
                writer.write(String.format("%-25s | %-30s | %-16s | %-8s | %-6s | %-7s | %s",
                        "Location", "Machine Name", "Owner", "Mode", "Val", "Loaded", "Reg.Time"));
                writer.newLine();
                writer.write("----------------------------------------------------------------------------------"); writer.newLine();

                // --- 写入数据行 ---
                for (Map.Entry<BlockPos, LazyTickStatCache> entry : resultList) {
                    BlockPos pos = entry.getKey();
                    LazyTickStatCache data = entry.getValue();
                    boolean isLoaded = sortContext.getLoadedPositions().contains(pos);

                    String locStr = String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
                    String modeStr = data.isForced() ? "Forced" : "Dynamic";
                    String valStr = String.valueOf(data.getScrollValue());
                    String loadStr = isLoaded ? "YES" : "NO";
                    String timeStr = String.valueOf(data.getRegisteredTime());

                    // 使用 truncate 防止过长的名字破坏表格排版
                    String line = String.format("%-25s | %-30s | %-16s | %-8s | %-6s | %-7s | %s",
                            locStr,
                            truncate(data.getBlockName(), 29),
                            truncate(data.getOwnerName(), 15),
                            modeStr, valStr, loadStr, timeStr);

                    writer.write(line);
                    writer.newLine();
                }
            }

            // --- 反馈结果 ---
            // 生成可点击的文件名组件 (点击复制)
            MutableComponent fileComp = Component.literal(fileName)
                    .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.AQUA)
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, fileName)));

            source.sendSuccess(() -> Component.literal("导出成功! 文件已保存至 dumps/createlazytick/ ")
                    .append(fileComp), true);

            return resultList.size();

        } catch (IOException e) {
            CreateLazyTick.LOGGER.error("Failed to write dump file", e);
            throw new SimpleCommandExceptionType(Component.literal("文件写入失败,请检查服务器日志")).create();
        }
    }

    // 字符串截断辅助方法
    private static String truncate(String s, int len) {
        if (s == null) return "null";
        if (s.length() <= len) return s;
        return s.substring(0, len - 3) + "...";
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

    // For clt list/reset NEAREST(生成相关位置和区块状态的上下文快照),与SortContext相关
    public static SortContext createSortContext(CommandSourceStack source, Set<BlockPos> targets) {
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
            int page, String sortStr, boolean globalReverse, String filterStr
    ) {

        // 4. 分页计算
        int totalMachines = sortedEntries.size();
        int totalPages = (int) Math.ceil((double) totalMachines / PAGE_SIZE);

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalMachines);

        // 5. 制作聊天栏标题
        LazyTickListRenderer.renderHeader(source, page, totalPages, totalMachines, sortStr);

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
        LazyTickListRenderer.renderNavBar(source, page, totalPages, sortStr, globalReverse, filterStr);
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
