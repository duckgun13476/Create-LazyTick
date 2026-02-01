package net.pinkcats.createlazytick.Register;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.helper.command.CommandHelper;
import net.pinkcats.createlazytick.helper.command.LazyTickSortMode;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LazyTickCommand {
    // 自动补全
    // list
    private static final SuggestionProvider<CommandSourceStack> SORT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(LazyTickSortMode.values())
                    .map(LazyTickSortMode::getId).collect(Collectors.toList()), builder);

    // reset
    private static final SuggestionProvider<CommandSourceStack> RESET_NAME_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        Set<String> names = ForcedActiveManager.getForcedMachines(level).values().stream()
                .map(LazyTickStatCache::getBlockName)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> RESET_OWNER_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        Set<String> owners = ForcedActiveManager.getForcedMachines(level).values().stream()
                .map(LazyTickStatCache::getOwnerName)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(owners, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("forced", "dynamic"), builder);

    private static final SuggestionProvider<CommandSourceStack> VALUE_OPERATOR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("equals", "biggerthan", "smallerthan"), builder);

    public static void RegisterCLTCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("createlazytick") // [1] 开始 createlazytick
                .requires(source -> source.hasPermission(2))

                // List
                .then(Commands.literal("list") // [2] 开始 list
                        .executes(ctx -> CommandHelper.onList(ctx, 1, LazyTickSortMode.DEFAULT, false))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)) // [3] 开始 page
                                .executes(ctx -> CommandHelper.onList(ctx, IntegerArgumentType.getInteger(ctx, "page"),
                                        LazyTickSortMode.DEFAULT, false))
                                .then(Commands.argument("sort", StringArgumentType.word()).suggests(SORT_SUGGESTIONS) // [4] 开始 sort
                                        .executes(ctx -> CommandHelper.onList(ctx,
                                                IntegerArgumentType.getInteger(ctx, "page"),
                                                LazyTickSortMode.byName(StringArgumentType.getString(ctx, "sort")),
                                                false))
                                        .then(Commands.argument("reverse", BoolArgumentType.bool()) // [5] 开始 reverse
                                                .executes(ctx -> CommandHelper.onList(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "page"),
                                                        LazyTickSortMode.byName(StringArgumentType.getString(ctx, "sort")),
                                                        BoolArgumentType.getBool(ctx, "reverse")
                                                ))
                                        ) // 结束 reverse
                                ) // 结束 sort
                        ) // 结束 page
                ) // 结束 list

                // Reset
                .then(Commands.literal("reset") // [6] 开始 reset
                        .then(Commands.literal("name")
                                .then(Commands.argument("block_name", ResourceLocationArgument.id())
                                        .suggests(RESET_NAME_SUGGESTIONS)
                                        .executes(CommandHelper::onResetByName)
                                )
                        )
                        .then(Commands.literal("player")
                                .then(Commands.argument("player_name", StringArgumentType.string())
                                        .suggests(RESET_OWNER_SUGGESTIONS)
                                        .executes(CommandHelper::onResetByPlayer)
                                )
                        )
                        .then(Commands.literal("mode")
                                .then(Commands.argument("mode_type", StringArgumentType.word())
                                        .suggests(MODE_SUGGESTIONS)
                                        .executes(CommandHelper::onResetByMode)
                                )
                        )
                        .then(Commands.literal("value")
                                .then(Commands.argument("operator", StringArgumentType.word())
                                        .suggests(VALUE_OPERATOR_SUGGESTIONS)
                                        .then(Commands.argument("target_value", IntegerArgumentType.integer(0, 100))
                                                .executes(CommandHelper::onResetByValue)
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                        .executes(CommandHelper::onResetByRadius)
                                )
                        )
                ) // 结束 reset
        );
    }


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
}