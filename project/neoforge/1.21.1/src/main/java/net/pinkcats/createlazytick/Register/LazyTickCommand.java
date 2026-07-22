package net.pinkcats.createlazytick.Register;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.helper.command.CommandExecutor;
import net.pinkcats.createlazytick.helper.command.CommandHelper;
import net.pinkcats.createlazytick.helper.command.FilterParser;
import net.pinkcats.createlazytick.helper.command.LazyTickSortMode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = CreateLazyTick.MODID)
public class LazyTickCommand {
    // 静态常量定义
    // =========================================================
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[\"{]+");

    private static final List<String> FILTER_KEYS = List.of(
            "id:", "name:",
            "operator:", "player:",
            "mode:",
            "value>", "value<", "value=", "value>=", "value<=",
            "time>", "time<"
    );

    private static final List<String> SORT_MODE_IDS = Arrays.stream(LazyTickSortMode.values())
            .map(LazyTickSortMode::getId)
            .toList();

    // 自动补全
    // =========================================================
    // list
    private static final SuggestionProvider<CommandSourceStack> SORT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(SORT_MODE_IDS, builder);

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

    public static final SuggestionProvider<CommandSourceStack> COMPLEX_FILTER_SUGGESTIONS = (context, builder) -> {
        // 别 删 注 释
        // e.g. /createlazytick list 1 "{mode,!name}" "{value>30, mode: dynamic , owner:hello5414}"
        //      /createlazytick list 1 "{mode,!name}" "{value>30, mode: <- you are here
        // 获取全段内容
        String fullInput = builder.getRemaining();  // "{value>30, mode:

        // 1. 定位光标当前所在的“单词” (只按逗号分隔,空格视为值的一部分,否则会导致补全异常)
        // 获取逗号所在的位置 // -> 8
        int lastCommandIndex = fullInput.lastIndexOf(',');
        // 提取当前片段(没找到逗号,则为第一个参数(提取全部),否则提取逗号后的部分(index+1,否则包含逗号)) //  mode(前面有1空格)
        String rawArg = (lastCommandIndex == -1) ? fullInput : fullInput.substring(lastCommandIndex + 1);

        // 2. 调整补全偏移量(定位补全位置) (只替换当前正在打的这一截,保留前面已输入的条件)
        // 挖去两端空格  // mode:
        String trimmedArg = rawArg.trim();
        Matcher prefixMatcher = PREFIX_PATTERN.matcher(trimmedArg);
        // 在当前片段判断前缀长度(前缀: " 或 { 或 "{ ),不是第一个片段则为0  // -> 0
        int prefixLength = prefixMatcher.find() ? prefixMatcher.group().length() : 0;

        // 挖去前缀  // mode:
        String currentArg = trimmedArg.substring(prefixLength);
        // 计算偏移  // -> 1
        int spaceOffset = rawArg.indexOf(trimmedArg);  // 左空格出现的位置(占用的字符数/造成的偏移)
        // 基准偏移 = 逗号后一位的位置 + 左空格的数量 + 前缀数量(前缀为0时,逗号位置必不为-1,反之成立) // -> 8 + 1 + 1 + 0 = 11(光标在(m)ode处)
        int baseOffset = ( lastCommandIndex + 1 ) + spaceOffset + prefixLength;

        // 如果还没有输入"{ ,强制输入
        if (prefixLength == 0 && lastCommandIndex == -1) {
            SuggestionsBuilder startBuilder = builder.createOffset(builder.getStart() + baseOffset);
            startBuilder.suggest("\"{");
            return startBuilder.buildFuture();
        }

        // 创建新的 Builder,应用正确的 Offset,之后实现正确的补全
        SuggestionsBuilder finalBuilder = builder.createOffset(builder.getStart() + baseOffset);

        // 3. 解析当前键值对
        Matcher matcher = FilterParser.TOKEN_PATTERN_FOR_SUGGESTION.matcher(currentArg);

        // 4. 根据目前正在输入的部分(键?运算符?值?)提供补全
        if (matcher.matches()) {
            String key = matcher.group(1).toLowerCase();
            String op = matcher.group(2);
            String val = matcher.group(3).toLowerCase();

            // 如果已输入引号开头,防御性剥离以匹配内容
            if (val.startsWith("\"")) val = val.substring(1);
            // 去空格
            String searchVal = val.trim();

            ServerLevel level = context.getSource().getLevel();

            switch (key) {
                // 根据键提示值
                // name/id (动态获取当前活跃机器(注册名))
                case "name", "id" -> {
                    // 从定时失效缓存中提取当前存在的机器 ID,缓存负责从ForcedActiveManager中提取id和owner
                    CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
                    Set<String> cachedMachineNames = cache.getMachineNames();

                    for (String id : cachedMachineNames) {
                        if (id.toLowerCase().contains(searchVal)) {
                            finalBuilder.suggest(key + op + id);
                        }
                    }
                }

                // 2. Owner / Player (在ForcedMachines中被记录过的玩家)
                case "operator", "player" -> {
                    CommandHelper.DimensionCache cache = CommandHelper.getDimensionMachineStatCache(level);
                    Set<String> cachedMachineOwners = cache.getMachineOwners();

                    for (String name : cachedMachineOwners) {
                        if (name.toLowerCase().startsWith(val)) {
                            finalBuilder.suggest(key + op + name);
                        }
                    }
                }

                // 3. Mode
                case "mode" -> {
                    if (searchVal.equals("forced") || searchVal.equals("dynamic")) {
                        // 创建一个临时的 builder 指向末尾
                        SuggestionsBuilder exactBuilder = builder.createOffset(builder.getStart() + baseOffset + currentArg.length());
                        exactBuilder.suggest(",");
                        exactBuilder.suggest("}\"");
                        return exactBuilder.buildFuture();
                    }
                    if ("forced".startsWith(searchVal)) finalBuilder.suggest(key + op + "forced");
                    if ("dynamic".startsWith(searchVal)) finalBuilder.suggest(key + op + "dynamic");
                }

                // 4. Value / Time (在值还没有被填入时显示补全,填入后立刻消失)
                case "value" -> {
                    if (val.isEmpty()) finalBuilder.suggest(key + op + "50");
                }
                case "time" -> {
                    if (val.isEmpty()) {
                        finalBuilder.suggest(key + op + "3d");
                        finalBuilder.suggest(key + op + "12h");
                        finalBuilder.suggest(key + op + "5d12h8m6s");
                    }
                }
            }
        } else {
            // 完全匹配不到K:V结构时提示键
            for (String k : FILTER_KEYS) {
                if (k.startsWith(currentArg.toLowerCase())) {
                    finalBuilder.suggest(k);
                }
            }
            // 空输入引导(提示最开头)
            if (currentArg.isEmpty()) {
                if (!fullInput.trim().startsWith("{") && !fullInput.trim().startsWith("\"")) {
                    finalBuilder.suggest("\"{");
                }
            }
        }

        return finalBuilder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> COMPLEX_SORT_SUGGESTIONS = (context, builder) -> {
        String fullInput = builder.getRemaining();

        // 1. 定位光标当前所在的“单词” (只按逗号分隔)
        int lastCommandIndex = fullInput.lastIndexOf(',');
        String rawArg = (lastCommandIndex == -1) ? fullInput : fullInput.substring(lastCommandIndex + 1);

        // 2. 计算前缀及纯净片段
        // 挖去空格
        String trimmedArg = rawArg.trim();
        Matcher prefixMatcher = PREFIX_PATTERN.matcher(trimmedArg);
        // 前缀长度
        int prefixLength = prefixMatcher.find() ? prefixMatcher.group().length() : 0;

        // 3. 调整补全偏移量(定位补全位置) (只替换当前正在打的这一截,保留前面已输入的条件)
        // 挖去前缀
        String currentArg = trimmedArg.substring(prefixLength);
        // 计算偏移
        int spaceOffset = rawArg.indexOf(trimmedArg);  // 空格造成的偏移(占用的字符数)
        int baseOffset = lastCommandIndex + 1 + spaceOffset + prefixLength;  // 基准偏移

        // 如果连前缀 "{ 都没有,强制只提示 "{和正向操作符,不显示取反操作符
        if (prefixLength == 0 && lastCommandIndex == -1) {
            SuggestionsBuilder startBuilder = builder.createOffset(builder.getStart() + baseOffset);
            startBuilder.suggest("\"{");
            for (String id : SORT_MODE_IDS) {
                // 正向
                if (id.toLowerCase().startsWith(currentArg.toLowerCase())) {
                    startBuilder.suggest(id);
                }
            }
            return startBuilder.buildFuture();
        }

        // 4. 提供补全建议
        boolean isExactMatch = false;
        for (String id : SORT_MODE_IDS) {
            if (currentArg.equalsIgnoreCase(id) || currentArg.equalsIgnoreCase("!" + id)) {
                isExactMatch = true;
                break;
            }
        }

        SuggestionsBuilder finalBuilder;

        if (isExactMatch) {
            finalBuilder = builder.createOffset(builder.getStart() + baseOffset + currentArg.length());
            finalBuilder.suggest(",");
            finalBuilder.suggest("}\"");
        } else {
            finalBuilder = builder.createOffset(builder.getStart() + baseOffset);
            for (String id : SORT_MODE_IDS) {
                // 正向
                if (id.toLowerCase().startsWith(currentArg.toLowerCase())) {
                    finalBuilder.suggest(id);
                }
                // 反向
                if (("!" + id).toLowerCase().startsWith(currentArg.toLowerCase())) {
                    finalBuilder.suggest("!" + id);
                }
            }
        }


        // 空输入引导(提示最开头)
        if (lastCommandIndex == -1 && currentArg.isEmpty()) {
            if (!fullInput.trim().startsWith("{") && !fullInput.trim().startsWith("\"")) {
                finalBuilder.suggest("\"{");
            }
        }

        return finalBuilder.buildFuture();
    };

    @SubscribeEvent
    public static void RegisterCLTCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("createlazytick") // [1] 开始 createlazytick
                .requires(source -> source.hasPermission(2))

                // List
                .then(Commands.literal("list") // 开始 list
                        .executes(ctx -> CommandExecutor.onListSimple(ctx, 1, LazyTickSortMode.DEFAULT, false))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)) // [3] 开始 page
                                .executes(ctx -> CommandExecutor.onListSimple(ctx, IntegerArgumentType.getInteger(ctx, "page"),
                                        LazyTickSortMode.DEFAULT, false))

                                .then(Commands.argument("sort", StringArgumentType.word()).suggests(SORT_SUGGESTIONS) // [4] 开始 sort
                                        .executes(ctx -> CommandExecutor.onListSimple(ctx,
                                                IntegerArgumentType.getInteger(ctx, "page"),
                                                LazyTickSortMode.byName(StringArgumentType.getString(ctx, "sort")),
                                                false))
                                        .then(Commands.argument("reverse", BoolArgumentType.bool()) // [5] 开始 reverse
                                                .executes(ctx -> CommandExecutor.onListSimple(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "page"),
                                                        LazyTickSortMode.byName(StringArgumentType.getString(ctx, "sort")),
                                                        BoolArgumentType.getBool(ctx, "reverse")
                                                ))
                                        ) // 结束 reverse
                                ) // 结束 sort

                                // 混合排序 /list <page> complex <sort> <reverse> <filter>
                                .then(Commands.literal("complex")
                                        .then(Commands.argument("sort_chain", StringArgumentType.string())
                                                .suggests(COMPLEX_SORT_SUGGESTIONS)
                                                .executes(ctx -> CommandExecutor.onListComplex(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "page"),
                                                        StringArgumentType.getString(ctx, "sort_chain"),
                                                        false, // 默认不反序
                                                        ""     // 默认无筛选
                                                ))
                                                .then(Commands.argument("global_reverse", BoolArgumentType.bool())
                                                        .executes(ctx -> CommandExecutor.onListComplex(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "page"),
                                                                StringArgumentType.getString(ctx, "sort_chain"),
                                                                BoolArgumentType.getBool(ctx, "global_reverse"),
                                                                ""
                                                        ))
                                                        .then(Commands.argument("filter_chain", StringArgumentType.greedyString())
                                                                .suggests(COMPLEX_FILTER_SUGGESTIONS)
                                                                .executes(ctx -> CommandExecutor.onListComplex(ctx,
                                                                        IntegerArgumentType.getInteger(ctx, "page"),
                                                                        StringArgumentType.getString(ctx, "sort_chain"),
                                                                        BoolArgumentType.getBool(ctx, "global_reverse"),
                                                                        StringArgumentType.getString(ctx, "filter_chain")
                                                                ))
                                                        ) // 结束 filter_chain
                                                ) // 结束 global_reserve
                                        ) // 结束 sort_chain
                                ) // 结束 complex(sort)
                        ) // 结束 page
                ) // 结束 list

                // Reset
                .then(Commands.literal("reset") // 开始 reset
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
                                        .suggests(COMPLEX_FILTER_SUGGESTIONS)
                                        .executes(CommandExecutor::onResetByComplex)
                                )
                        )
                ) // 结束 reset

                // Limit (Permission System)
                .then(Commands.literal("limit") // 开始 limit
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

                // Dump
                .then(Commands.literal("dump")
                        .executes(ctx -> CommandExecutor.onDump(ctx, "default", false, ""))
                        .then(Commands.argument("sort_chain", StringArgumentType.string())
                                .suggests(COMPLEX_SORT_SUGGESTIONS)
                                .executes(ctx -> CommandExecutor.onDump(ctx,
                                        StringArgumentType.getString(ctx, "sort_chain"),
                                        false,
                                        ""))
                                .then(Commands.argument("global_reverse", BoolArgumentType.bool())
                                        .executes(ctx -> CommandExecutor.onDump(ctx,
                                                StringArgumentType.getString(ctx, "sort_chain"),
                                                BoolArgumentType.getBool(ctx, "global_reverse"),
                                                ""))
                                        .then(Commands.argument("filter_chain", StringArgumentType.greedyString())
                                                .suggests(COMPLEX_FILTER_SUGGESTIONS)
                                                .executes(ctx -> CommandExecutor.onDump(ctx,
                                                        StringArgumentType.getString(ctx, "sort_chain"),
                                                        BoolArgumentType.getBool(ctx, "global_reverse"),
                                                        StringArgumentType.getString(ctx, "filter_chain")
                                                ))
                                        ) // 结束 filter_chain
                                ) // 结束 global_reserve
                        ) // 结束 sort_chain
                ) //结束 dump
        );
    }
}