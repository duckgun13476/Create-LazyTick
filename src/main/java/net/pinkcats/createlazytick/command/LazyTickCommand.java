package net.pinkcats.createlazytick.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.*;

public class LazyTickCommand {

    private static final int PAGE_SIZE = 15;

    private static List<Map.Entry<BlockPos, LazyTickStatCache>> cachedSortedList = null;
    private static long cachedVersion = -1;
    private static ResourceKey<Level> cachedDimension = null;

    public static void RegisterCLTCommand(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("createlazytick")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(context -> listForcedMachines(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> listForcedMachines(context, IntegerArgumentType.getInteger(context, "page")))
                        )
                )
        );
    }

    private static int listForcedMachines(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        ResourceKey<Level> currentDimension = level.dimension();
        long currentVersion = ForcedActiveManager.getVersion();

        List<Map.Entry<BlockPos, LazyTickStatCache>> sortedEntries;

        if (cachedSortedList != null && cachedVersion == currentVersion && currentDimension.equals(cachedDimension)) {
            sortedEntries = cachedSortedList;
        } else {
            Map<BlockPos, LazyTickStatCache> forcedMachines = ForcedActiveManager.getForcedMachines(level);

            if (forcedMachines.isEmpty()) {
                source.sendSystemMessage(Component.literal("当前名单中没有非默认配置的机器").withStyle(ChatFormatting.GREEN));
                cachedSortedList = new ArrayList<>();
                cachedVersion = currentVersion;
                cachedDimension = currentDimension;
                return 2;
            }

            sortedEntries = new ArrayList<>(forcedMachines.entrySet());

            // [修复] 使用 Lambda 替代方法引用，解决解析问题
            sortedEntries.sort(Comparator.comparingInt(
                    (Map.Entry<BlockPos, LazyTickStatCache> e) -> e.getKey().getX())
                    .thenComparingInt(e -> e.getKey().getY())
                    .thenComparingInt(e -> e.getKey().getZ()));

            cachedSortedList = sortedEntries;
            cachedVersion = currentVersion;
            cachedDimension = currentDimension;
        }

        int totalMachines = sortedEntries.size();

        if (totalMachines == 0) {
            source.sendSystemMessage(Component.literal("当前名单中没有非默认配置的机器").withStyle(ChatFormatting.GREEN));
            return 1;
        }

        int totalPages = (int) Math.ceil((double) totalMachines / PAGE_SIZE);

        // 变量 page 在这里被修改了，所以它不再是 effective final
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalMachines);

        MutableComponent header = Component.literal("=== 非默认状态机器名单 (第 " + page + "/" + totalPages + " 页 | 共 " + totalMachines + " 个) ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        source.sendSystemMessage(header);

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<BlockPos, LazyTickStatCache> entry = sortedEntries.get(i);
            BlockPos pos = entry.getKey();
            LazyTickStatCache info = entry.getValue();

            // 只有当区块已加载时,才去检查机器是否还在
            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);

                // BE不存在(null)/不是mixin的SmartBE/处于默认状态
                if (!(be instanceof ISmartBlockEntityControl control) || control.lazytick$isDefaultState()) {

                    // 清理失效数据
                    ForcedActiveManager.unregister(level, pos);

                    //source.sendSystemMessage(Component.literal("已自动清理失效记录: " + pos.toShortString()).withStyle(ChatFormatting.RED));
                    continue;
                }
            }

            boolean isLoaded = level.isLoaded(pos);
            String statusPrefix = isLoaded ? "" : "[未加载] ";
            ChatFormatting nameColor = isLoaded ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA;

            String modeStr = info.isForced() ? "强制锁定" : "动态控制";
            ChatFormatting modeColor = info.isForced() ? ChatFormatting.RED : ChatFormatting.BLUE;

            MutableComponent hoverText = Component.literal("详细信息:\n")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("模式状态: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(modeStr + "\n").withStyle(modeColor))
                    .append(Component.literal("注册时间: " + info.getFormattedTime() + "\n").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("设定数值: " + info.getScrollValue() + "\n").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("点击传送").withStyle(ChatFormatting.GREEN));

            String tpCommand = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

            //直接使用 info.getBlockName()，不需要去getBlockEntity查
            MutableComponent listEntry = Component.literal(" " + (i + 1) + ". ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(statusPrefix + info.getBlockName())
                            .withStyle(nameColor))
                    .append(Component.literal(" [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                            .withStyle(ChatFormatting.GREEN)
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)) // 应用 Hover
                            )
                    )
                    .append(Component.literal(" (操作者: " + info.getOwnerName() + ")").withStyle(ChatFormatting.DARK_GRAY));

            source.sendSystemMessage(listEntry);
        }

        source.sendSystemMessage(Component.literal("")); // 空行分隔

        MutableComponent navBar = Component.literal("      "); // 缩进

        // 1. 上一页按钮
        if (page > 1) {
            final int prevPage = page - 1;
            navBar.append(Component.literal("<<< 上一页")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/createlazytick list " + prevPage))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击前往第 " + prevPage + " 页")))
                    ));
        } else {
            navBar.append(Component.literal("<<< 上一页").withStyle(ChatFormatting.DARK_GRAY)); // 禁用状态
        }

        // 2. 分隔符
        navBar.append(Component.literal("   |   ").withStyle(ChatFormatting.GRAY));

        // 3. 下一页按钮
        if (page < totalPages) {
            final int nextPage = page + 1;
            navBar.append(Component.literal("下一页 >>>")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/createlazytick list " + nextPage))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击前往第 " + nextPage + " 页")))
                    ));
        } else {
            navBar.append(Component.literal("下一页 >>>").withStyle(ChatFormatting.DARK_GRAY)); // 禁用状态
        }
        source.sendSystemMessage(navBar);
        return 1;
    }
}