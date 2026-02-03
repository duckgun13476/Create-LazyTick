package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;
import java.util.function.Predicate;

public class CommandHelper {
    private static final int PAGE_SIZE = 15;

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

    // 静态类(快照)  For NEAREST
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

    // clt list/reset
    // For NEAREST(在LazyTickCommand使用,生成相关位置快照)
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

    //clt list
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


    public static long parseDuration(String input) throws NumberFormatException {
        if (input.length() < 2) throw new NumberFormatException("格式过短");

        // 获取最后一位作为单位 (d/h/m/s)
        char unit = input.charAt(input.length() - 1);
        // 获取前面的数字部分
        String numberStr = input.substring(0, input.length() - 1);
        long number = Long.parseLong(numberStr);

        return switch (Character.toLowerCase(unit)) {
            case 'd' -> number * 24 * 60 * 60 * 1000L; // 天 -> 毫秒
            case 'h' -> number * 60 * 60 * 1000L;      // 时 -> 毫秒(下面以此类推)
            case 'm' -> number * 60 * 1000L;
            case 's' -> number * 1000L;
            default -> throw new NumberFormatException("未知单位");
        };
    }


}
