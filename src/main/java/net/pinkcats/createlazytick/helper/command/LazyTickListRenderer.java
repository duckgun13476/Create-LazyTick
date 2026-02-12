package net.pinkcats.createlazytick.helper.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Map;

public class LazyTickListRenderer {

    // 渲染标题头
    public static void renderHeader(CommandSourceStack source, int page, int totalPages,
                                    int total, String sortStr) {
        String safeSortStr = (sortStr == null || sortStr.isEmpty()) ? "default" : sortStr;

        MutableComponent sortDisplayComp;
        String lowerSort = safeSortStr.toLowerCase();

        switch (lowerSort) {
            case "default" -> sortDisplayComp = Component.translatable("createlazytick.sort.default");
            case "nearest" -> sortDisplayComp = Component.translatable("createlazytick.sort.nearest");
            case "time"    -> sortDisplayComp = Component.translatable("createlazytick.sort.time");
            case "name"    -> sortDisplayComp = Component.translatable("createlazytick.sort.name");
            case "player"  -> sortDisplayComp = Component.translatable("createlazytick.sort.player");
            case "mode"    -> sortDisplayComp = Component.translatable("createlazytick.sort.mode");
            case "value"   -> sortDisplayComp = Component.translatable("createlazytick.sort.value");
            case "loaded"  -> sortDisplayComp = Component.translatable("createlazytick.sort.loaded");
            default -> {
                MutableComponent hover = Component.translatable(
                        "createlazytick.sort.hover_logic",
                        safeSortStr
                );

                sortDisplayComp = Component.translatable("createlazytick.sort.composite")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style.withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)
                        ));
            }
        }

        MutableComponent header = Component.translatable(
                "createlazytick.list.header.non_default",
                page,
                totalPages,
                total,
                sortDisplayComp
        ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        source.sendSystemMessage(header);
    }

    // 渲染单行条目
    public static void renderItem(CommandSourceStack source, int index,
                                  Map.Entry<BlockPos, LazyTickStatCache> entry, boolean isLoaded) {
        BlockPos pos = entry.getKey();
        LazyTickStatCache info = entry.getValue();


        ChatFormatting nameColor = isLoaded ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA;

        // 获取本地方块名称
        Component localizedName = info.getDisplayName();

        // 构建悬停文本 (HoverText)

        ChatFormatting modeColor = info.isForced() ? ChatFormatting.RED : ChatFormatting.AQUA;

        String modeKey = info.isForced()
                ? "createlazytick.mode.forced_locked"
                : "createlazytick.mode.dynamic_control";

        MutableComponent modeValue = Component.translatable(modeKey).withStyle(modeColor);

        MutableComponent hoverText = Component.translatable("createlazytick.hover.details")
                .withStyle(ChatFormatting.YELLOW)
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.mode", modeValue)
                        .withStyle(ChatFormatting.GRAY))
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.machine", localizedName.copy())
                        .withStyle(ChatFormatting.GRAY))
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.last_operator", info.getOwnerName())
                        .withStyle(ChatFormatting.GRAY))
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.registered_time", info.getFormattedTime())
                        .withStyle(ChatFormatting.GRAY))
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.scroll_value", info.getScrollValue())
                        .withStyle(ChatFormatting.GRAY))
                .append(mes.enter())

                .append(Component.translatable("createlazytick.hover.click_teleport")
                        .withStyle(ChatFormatting.GREEN));

        String tpCommand = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

        // 构建列表行(1行)
        MutableComponent listEntry = mes.CharM(" " + index + ". ")
                .withStyle(ChatFormatting.GRAY)
                .append((isLoaded ? Component.empty()
                        : Component.translatable("createlazytick.status.not_loaded_prefix"))
                        .copy().withStyle(nameColor))
                .append(localizedName.copy().withStyle(nameColor))
                .append(mes.CharM(" [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                        )
                )
                .append(mes.CharM(" (" + info.getOwnerName() + ")").withStyle(ChatFormatting.DARK_GRAY));

        source.sendSystemMessage(listEntry);
    }

    // 渲染翻页导航栏(内部生成带参数自动命令)
    public static void renderNavBar(CommandSourceStack source, int page, int totalPages,
                                    String sortStr, boolean isReverse, String filterStr) {
        source.sendSystemMessage(mes.spaces(0)); // 空行分隔
        MutableComponent navBar = (MutableComponent) mes.spaces(6); // 缩进

        // 上一页
        if (page > 1) {
            appendNavButton(navBar, "createlazytick.nav.prev", page - 1, sortStr, isReverse, filterStr);
        } else {
            navBar.append(Component.translatable("createlazytick.nav.prev").withStyle(ChatFormatting.DARK_GRAY)); // 不可用状态
        }
        navBar.append(mes.CharM("   |   ").withStyle(ChatFormatting.GRAY));

        // 下一页
        if (page < totalPages) {
            appendNavButton(navBar, "createlazytick.nav.next", page + 1, sortStr, isReverse, filterStr);
        } else {
            navBar.append(Component.translatable("createlazytick.nav.next").withStyle(ChatFormatting.DARK_GRAY));
        }

        source.sendSystemMessage(navBar);
    }

    // 生成翻页按钮
    private static void appendNavButton(MutableComponent parent, String textKey, int targetPage,
                                        String sortStr, boolean isReverse, String filterStr) {
        String safeSort = (sortStr == null || sortStr.isEmpty()) ? "default" : sortStr;
        String safeFilter = (filterStr == null || filterStr.isEmpty()) ? "{}" : filterStr;

        safeSort = quoteIfNecessary(safeSort);
        safeFilter = quoteIfNecessary(safeFilter);

        // /createlazytick list <page> <sortMode> <isReverse> <filter>
        String command = "/createlazytick list " + targetPage + " complex " + safeSort + " " + isReverse + " " + safeFilter;

        parent.append(Component.translatable(textKey)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("createlazytick.nav.hover_go_page", targetPage)
                        ))
                ));
    }

    private static String quoteIfNecessary(String input) {
        if (input.contains(" ")) {
            // 如果包含空格,必须包裹引号,并且把内部原有的引号转义
            return "\"" + input.replace("\"", "\\\"") + "\"";
        }
        return input;
    }
}