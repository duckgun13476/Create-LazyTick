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
import net.pinkcats.createlazytick.helper.command.LazyTickSortMode;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;
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

    private static final SuggestionProvider<CommandSourceStack> TIME_OPERATOR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of("olderthan", "newerthan"), builder);

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