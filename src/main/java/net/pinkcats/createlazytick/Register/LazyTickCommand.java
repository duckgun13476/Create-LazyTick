package net.pinkcats.createlazytick.Register;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.pinkcats.createlazytick.helper.command.CommandExecutor;
import net.pinkcats.createlazytick.helper.command.CommandHelper;
import net.pinkcats.createlazytick.helper.command.LazyTickSortMode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LazyTickCommand {
    // 静态常量定义
    // =========================================================
    private static final Pattern COMPLEX_PATTERN = Pattern.compile("^([a-zA-Z]+)(:|>=|<=|>|<|=)(.*)");

    private static final List<String> COMPLEX_KEYS = List.of(
            "id:", "name:",
            "operator:", "player:",
            "mode:",
            "value>", "value<", "value=", "value>=", "value<=",
            "time>", "time<"
    );

    // 自动补全
    // =========================================================
    // list
    private static final SuggestionProvider<CommandSourceStack> SORT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(LazyTickSortMode.values())
                    .map(LazyTickSortMode::getId).collect(Collectors.toList()), builder);

    // reset
    private static final SuggestionProvider<CommandSourceStack> RESET_NAME_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
        return SharedSuggestionProvider.suggest(cache.getMachineNames(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> RESET_OWNER_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
        return SharedSuggestionProvider.suggest(cache.getMachineOwners(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("forced", "dynamic"), builder);

    private static final SuggestionProvider<CommandSourceStack> VALUE_OPERATOR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("equals", "biggerthan", "smallerthan"), builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_OPERATOR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("olderthan", "newerthan"), builder);

    public static final SuggestionProvider<CommandSourceStack> COMPLEX_SUGGESTIONS = (context, builder) -> {
        String fullInput = builder.getRemaining();

        // 1. 定位光标当前所在的“单词” (最后一个空格后的内容)
        int lastSpaceIndex = fullInput.lastIndexOf(' ');
        String currentArg = (lastSpaceIndex == -1) ? fullInput : fullInput.substring(lastSpaceIndex + 1);

        // 2. 调整补全偏移量(定位补全位置) (只替换当前正在打的这一截,保留前面已输入的条件)
        builder = builder.createOffset(builder.getStart() + lastSpaceIndex + 1);

        // 3. 解析当前键值对
        Matcher matcher = COMPLEX_PATTERN.matcher(currentArg);

        // 4. 根据目前正在输入的部分(键?运算符?值?)提供补全
        if (matcher.matches()) {
            String key = matcher.group(1).toLowerCase();
            String op = matcher.group(2);
            String val = matcher.group(3).toLowerCase();

            ServerLevel level = context.getSource().getLevel();

            switch (key) {
                // 根据键提示值
                // name/id (动态获取当前活跃机器(注册名))
                case "name", "id" -> {
                    // 从定时失效缓存中提取当前存在的机器 ID,缓存负责从ForcedActiveManager中提取id和owner
                    CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
                    Set<String> cachedMachineNames = cache.getMachineNames();

                    for (String id : cachedMachineNames) {
                        if (id.toLowerCase().contains(val)) {
                            builder.suggest(key + op + id);
                        }
                    }
                }

                // 2. Owner / Player (在ForcedMachines中被记录过的玩家)
                case "operator", "player" -> {
                    CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
                    Set<String> cachedMachineOwners = cache.getMachineOwners();

                    for (String name : cachedMachineOwners) {
                        if (name.toLowerCase().startsWith(val)) {
                            builder.suggest(key + op + name);
                        }
                    }
                }

                // 3. Mode
                case "mode" -> {
                    if ("forced".startsWith(val)) builder.suggest(key + op + "forced");
                    if ("dynamic".startsWith(val)) builder.suggest(key + op + "dynamic");
                }

                // 4. Value / Time (在值还没有被填入时显示补全,填入后立刻消失)
                case "value" -> {
                    if (val.isEmpty()) builder.suggest(key + op + "50");
                }
                case "time" -> {
                    if (val.isEmpty()) {
                        builder.suggest(key + op + "3d");
                        builder.suggest(key + op + "12h");
                        builder.suggest(key + op + "5d12h8m6s");
                    }
                }
            }
        } else {
            // 提示键
            for (String k : COMPLEX_KEYS) {
                if (k.startsWith(currentArg.toLowerCase())) {
                    builder.suggest(k);
                }
            }
        }

        return builder.buildFuture();
    };

    public static void RegisterCLTCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("createlazytick") // [1] 开始 createlazytick
                .requires(source -> source.hasPermission(2))

                // List
                .then(Commands.literal("list") // [2] 开始 list
                        .executes(ctx -> CommandExecutor.onList(ctx, 1, LazyTickSortMode.DEFAULT, false))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)) // [3] 开始 page
                                .executes(ctx -> CommandExecutor.onList(ctx, IntegerArgumentType.getInteger(ctx, "page"),
                                        LazyTickSortMode.DEFAULT, false))
                                .then(Commands.argument("sort", StringArgumentType.word()).suggests(SORT_SUGGESTIONS) // [4] 开始 sort
                                        .executes(ctx -> CommandExecutor.onList(ctx,
                                                IntegerArgumentType.getInteger(ctx, "page"),
                                                LazyTickSortMode.byName(StringArgumentType.getString(ctx, "sort")),
                                                false))
                                        .then(Commands.argument("reverse", BoolArgumentType.bool()) // [5] 开始 reverse
                                                .executes(ctx -> CommandExecutor.onList(ctx,
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
                                        .executes(CommandExecutor::onResetByName)
                                )
                        )
                        .then(Commands.literal("player")
                                .then(Commands.argument("player_name", StringArgumentType.string())
                                        .suggests(RESET_OWNER_SUGGESTIONS)
                                        .executes(CommandExecutor::onResetByPlayer)
                                )
                        )
                        .then(Commands.literal("mode")
                                .then(Commands.argument("mode_type", StringArgumentType.word())
                                        .suggests(MODE_SUGGESTIONS)
                                        .executes(CommandExecutor::onResetByMode)
                                )
                        )
                        .then(Commands.literal("value")
                                .then(Commands.argument("operator", StringArgumentType.word())
                                        .suggests(VALUE_OPERATOR_SUGGESTIONS)
                                        .then(Commands.argument("target_value", IntegerArgumentType.integer(0, 100))
                                                .executes(CommandExecutor::onResetByValue)
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                        .executes(CommandExecutor::onResetByRadius)
                                )
                        )
                        .then(Commands.literal("time")
                                .then(Commands.argument("operator", StringArgumentType.word())
                                        .suggests(TIME_OPERATOR_SUGGESTIONS)
                                        .then(Commands.argument("duration", StringArgumentType.string()) // 接收"3d","12h"等
                                                .executes(CommandExecutor::onResetByTime)
                                        )
                                )
                        )
                        .then(Commands.literal("complex")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .suggests(LazyTickCommand.COMPLEX_SUGGESTIONS)
                                        .executes(CommandExecutor::onResetByComplex)
                                )
                        )
                ) // 结束 reset

                // Limit (Permission System)
                .then(Commands.literal("limit") // [7] 开始 limit
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0)) // 禁止负数
                                                .executes(ctx -> CommandExecutor.onLimitSet(ctx,
                                                        GameProfileArgument.getGameProfiles(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> CommandExecutor.onLimitRemove(ctx,
                                                GameProfileArgument.getGameProfiles(ctx, "player")))
                                )
                        )
                        .then(Commands.literal("check")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> CommandExecutor.onLimitCheck(ctx,
                                                GameProfileArgument.getGameProfiles(ctx, "player")))
                                )
                        )
                ) // 结束 limit
        );
    }
}