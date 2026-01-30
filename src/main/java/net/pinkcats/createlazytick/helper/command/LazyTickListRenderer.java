package net.pinkcats.createlazytick.helper.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Map;

public class LazyTickListRenderer {

    // 渲染标题头
    public static void renderHeader(CommandSourceStack source, int page, int totalPages, int total, LazyTickSortMode sortMode) {
        String sortName = switch (sortMode) {
            case DEFAULT -> "默认";
            case NEAREST -> "距离";
            case TIME -> "时间";
            case NAME -> "机名";
            case PLAYER -> "玩家";
            case MODE -> "模式";
            case VALUE -> "数值";
        };

        MutableComponent header = Component.literal("=== 非默认懒加载机器名单 (第 " + page + "/" + totalPages + " 页 | 共 " + total + " 个 | 排序: " + sortName + ") ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        source.sendSystemMessage(header);
    }

    // 渲染单行条目
    public static void renderItem(CommandSourceStack source, int index, Map.Entry<BlockPos, LazyTickStatCache> entry, boolean isLoaded) {
        BlockPos pos = entry.getKey();
        LazyTickStatCache info = entry.getValue();

        String statusPrefix = isLoaded ? "" : "[未加载] ";
        ChatFormatting nameColor = isLoaded ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA;

        // 构建悬停文本 (HoverText)
        String modeStr = info.isForced() ? "强制锁定" : "动态控制";
        ChatFormatting modeColor = info.isForced() ? ChatFormatting.RED : ChatFormatting.AQUA;

        MutableComponent hoverText = Component.literal("详细信息:\n")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("模式状态: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(modeStr + "\n").withStyle(modeColor))
                .append(Component.literal("机器名称: " + info.getBlockName() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("拥有者: " + info.getOwnerName() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("注册时间: " + info.getFormattedTime() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("设定数值: " + info.getScrollValue() + "%\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("点击传送").withStyle(ChatFormatting.GREEN));

        String tpCommand = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

        // 构建列表行(1行)
        MutableComponent listEntry = Component.literal(" " + index + ". ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(statusPrefix + info.getBlockName())
                        .withStyle(nameColor))
                .append(Component.literal(" [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                        )
                )
                .append(Component.literal(" (" + info.getOwnerName() + ")").withStyle(ChatFormatting.DARK_GRAY));

        source.sendSystemMessage(listEntry);
    }

    // 渲染翻页导航栏(内部生成带参数自动命令)
    public static void renderNavBar(CommandSourceStack source, int page, int totalPages, LazyTickSortMode sortMode, boolean isReverse) {
        source.sendSystemMessage(Component.literal("")); // 空行分隔

        MutableComponent navBar = Component.literal("      "); // 缩进

        // 上一页
        if (page > 1) {
            appendNavButton(navBar, "<<< 上一页", page - 1, sortMode, isReverse);
        } else {
            navBar.append(Component.literal("<<< 上一页").withStyle(ChatFormatting.DARK_GRAY));
        }

        navBar.append(Component.literal("   |   ").withStyle(ChatFormatting.GRAY));

        // 下一页
        if (page < totalPages) {
            appendNavButton(navBar, "下一页 >>>", page + 1, sortMode, isReverse);
        } else {
            navBar.append(Component.literal("下一页 >>>").withStyle(ChatFormatting.DARK_GRAY));
        }

        source.sendSystemMessage(navBar);
    }

    // 生成翻页按钮
    private static void appendNavButton(MutableComponent parent, String text, int targetPage, LazyTickSortMode sortMode, boolean isReverse) {
        // 此处需完整命令
        // /createlazytick list <page> <sortMode> <isReverse>
        String command = "/createlazytick list " + targetPage + " " + sortMode.getId() + " " + isReverse;

        parent.append(Component.literal(text)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击前往第 " + targetPage + " 页")))
                ));
    }
}