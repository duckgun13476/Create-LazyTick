package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.Register.LazyTickCommand;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;

public class CommandHelper {
    private static final int PAGE_SIZE = 15;

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

                    // (调试用,暂不确定是否正式加入)
                    source.sendSystemMessage(Component.literal("已自动清理失效记录: " + pos.toShortString()).withStyle(ChatFormatting.RED));
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


    private static long parseDuration(String input) throws NumberFormatException {
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

    public static int onResetByName(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation rl = ctx.getArgument("block_name", ResourceLocation.class);
        String id = rl.toString();

        Block block = ForgeRegistries.BLOCKS.getValue(rl);

        Component nameComponent;
        if (block != null && block != Blocks.AIR) {
            nameComponent = block.getName(); // 获取翻译组件
        } else {
            nameComponent = Component.literal(id); // 降级为 ID
        }

        Component desc = Component.literal("名称 [")
                .append(nameComponent.copy().withStyle(ChatFormatting.AQUA))
                .append("]");

        return LazyTickCommand.executeReset(ctx, desc,
                entry -> entry.getValue().getBlockName().equals(id));
    }

    public static int onResetByPlayer(CommandContext<CommandSourceStack> ctx) {
        String owner = StringArgumentType.getString(ctx, "player_name");
        Component desc = Component.literal("所有者 [" + owner + "]");
        return LazyTickCommand.executeReset(ctx,  desc,
                entry -> entry.getValue().getOwnerName().equals(owner));
    }

    public static int onResetByMode(CommandContext<CommandSourceStack> ctx) {
        String modeStr = StringArgumentType.getString(ctx, "mode_type");
        boolean isForced = modeStr.equalsIgnoreCase("forced");
        Component desc = Component.literal("模式 [" + (isForced ? "强制" : "动态") + "]");
        return LazyTickCommand.executeReset(ctx, desc,
                entry -> entry.getValue().isForced() == isForced);
    }

    public static int onResetByValue(CommandContext<CommandSourceStack> ctx) {
        String operator = StringArgumentType.getString(ctx, "operator");
        int targetVal = IntegerArgumentType.getInteger(ctx, "target_value");

        String displaySymbol;
        java.util.function.BiPredicate<Integer, Integer> logic; //Predicate 逻辑

        switch (operator.toLowerCase()) {
            case "biggerthan" -> {
                displaySymbol = ">";
                logic = (current, target) -> current > target;
            }
            case "smallerthan" -> {
                displaySymbol = "<";
                logic = (current, target) -> current < target;
            }
            case "equals" -> {
                displaySymbol = "=";
                logic = Integer::equals;
            }
            default -> {
                ctx.getSource().sendFailure(Component.literal("未知的操作符: " + operator));
                return 0;
            }
        }
        Component desc = Component.literal("数值 [" + displaySymbol + " " + targetVal + "]");
        return LazyTickCommand.executeReset(ctx, desc, entry -> {
            // Never negative (-)
            int currentVal = entry.getValue().getScrollValue();
            return logic.test(currentVal, targetVal);
        });
    }

    public static int onResetByRadius(CommandContext<CommandSourceStack> ctx) {
        int range = IntegerArgumentType.getInteger(ctx, "range");
        BlockPos center = BlockPos.containing(ctx.getSource().getPosition());
        double rangeSqr = range * range;

        Component desc = Component.literal("半径 [" + range + "格]");
        return LazyTickCommand.executeReset(ctx, desc,
                entry -> entry.getKey().distToCenterSqr(center.getX(), center.getY(), center.getZ()) <= rangeSqr);
    }

    public static int onResetByTime(CommandContext<CommandSourceStack> ctx) {
        String operator = StringArgumentType.getString(ctx, "operator");
        String durationStr = StringArgumentType.getString(ctx, "duration");

        long targetDuration;
        try {
            targetDuration = parseDuration(durationStr);
        } catch (NumberFormatException e) {
            ctx.getSource().sendFailure(Component.literal("时间格式错误: " + durationStr + " (示例: 3d; 12h; 30m; 6s)"));
            return 0;
        }

        // 准备显示符号和逻辑
        String displaySymbol;
        java.util.function.BiPredicate<Long, Long> logic;

        switch (operator.toLowerCase()) {
            case "olderthan" -> {
                displaySymbol = ">";
                // "存在时长(Age)" 大于 "目标时长" => 比对应目标时间戟更早/更旧
                logic = (age, target) -> age > target;
            }
            case "newerthan" -> {
                displaySymbol = "<";
                // "存在时长(Age)" 小于 "目标时长" => 比对应目标时间戟更晚/更新
                logic = (age, target) -> age < target;
            }
            default -> {
                ctx.getSource().sendFailure(Component.literal("时间筛选仅支持 olderthan (早于) 或 newerthan (晚于)"));
                return 0;
            }
        }
        Component desc = Component.literal("注册时长 [" + displaySymbol + " " + durationStr + "]");

        long now = System.currentTimeMillis();

        return LazyTickCommand.executeReset(ctx, desc, entry -> {
            long registeredTime = entry.getValue().getRegisteredTime();

            if (registeredTime <= 0) return false;  // 非法数据

            // 计算由注册至今经过的时长 (Age)
            long age = now - registeredTime;

            return logic.test(age, targetDuration);
        });
    }

    public static int onList(CommandContext<CommandSourceStack> ctx, int page, LazyTickSortMode sort, boolean reverse) {
        return LazyTickCommand.executeList(ctx, page, sort, reverse);
    }
}
