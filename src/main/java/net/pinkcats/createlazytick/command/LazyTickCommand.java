package net.pinkcats.createlazytick.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class LazyTickCommand {

    private static final int PAGE_SIZE = 15;

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

        Set<BlockPos> forcedPositions = ForcedActiveManager.getForcedPositions(level);

        if (forcedPositions.isEmpty()) {
            source.sendSystemMessage(Component.literal("当前已加载区块中没有被强制活跃的机器。").withStyle(ChatFormatting.GREEN));
            return 2;
        }

        List<BlockPos> sortedPos = new ArrayList<>(forcedPositions);

        // [修复] 使用 Lambda 替代方法引用，解决解析问题
        sortedPos.sort(Comparator.comparingInt((BlockPos p) -> p.getX())
                .thenComparingInt(Vec3i::getY)
                .thenComparingInt(Vec3i::getZ));

        int totalMachines = sortedPos.size();
        int totalPages = (int) Math.ceil((double) totalMachines / PAGE_SIZE);

        // 变量 page 在这里被修改了，所以它不再是 effective final
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalMachines);

        MutableComponent header = Component.literal("=== 强制活跃机器名单 (第 " + page + "/" + totalPages + " 页 | 共 " + totalMachines + " 个) ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        source.sendSystemMessage(header);

        for (int i = startIndex; i < endIndex; i++) {
            BlockPos pos = sortedPos.get(i);
            BlockEntity be = level.getBlockEntity(pos);

            String blockName;

            String operator = "未知";

            if (be != null) {
                blockName = be.getBlockState().getBlock().getName().getString();
                if (be instanceof ISmartBlockEntityControl control) {
                    operator = control.createLazyTick$getUserName();
                    if (operator.isEmpty()) operator = "未知";
                }
            } else {
                blockName = "已失效/未加载";
            }

            String tpCommand = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

            MutableComponent entry = Component.literal(" " + (i + 1) + ". ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(blockName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                            .withStyle(ChatFormatting.GREEN)
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击传送到此位置")))
                            )
                    )
                    .append(Component.literal(" (操作者: " + operator + ")").withStyle(ChatFormatting.DARK_GRAY));

            source.sendSystemMessage(entry);
        }

        if (page < totalPages) {
            // [修复] 创建一个新的 final 变量供 lambda 使用
            final int nextPage = page + 1;

            MutableComponent nextPageBtn = Component.literal(">>> 点击查看下一页 >>>")
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/createlazytick list " + nextPage))
                    );
            source.sendSystemMessage(nextPageBtn);
        }

        return 1;
    }
}