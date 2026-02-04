package net.pinkcats.createlazytick.helper.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import net.pinkcats.createlazytick.manager.LazyTickSavedLimitList;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class CommandExecutor {
    public static int onList(CommandContext<CommandSourceStack> ctx, int page, LazyTickSortMode sort, boolean reverse) {
        return CommandHelper.executeList(ctx, page, sort, reverse);
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

        return CommandHelper.executeReset(ctx, desc,
                entry -> entry.getValue().getBlockName().equals(id));
    }

    public static int onResetByPlayer(CommandContext<CommandSourceStack> ctx) {
        String owner = StringArgumentType.getString(ctx, "player_name");
        Component desc = Component.literal("所有者 [" + owner + "]");
        return CommandHelper.executeReset(ctx,  desc,
                entry -> entry.getValue().getOwnerName().equals(owner));
    }

    public static int onResetByMode(CommandContext<CommandSourceStack> ctx) {
        String modeStr = StringArgumentType.getString(ctx, "mode_type");
        boolean isForced = modeStr.equalsIgnoreCase("forced");
        Component desc = Component.literal("模式 [" + (isForced ? "强制" : "动态") + "]");
        return CommandHelper.executeReset(ctx, desc,
                entry -> entry.getValue().isForced() == isForced);
    }

    public static int onResetByValue(CommandContext<CommandSourceStack> ctx) {
        String operator = StringArgumentType.getString(ctx, "operator");
        int targetVal = IntegerArgumentType.getInteger(ctx, "target_value");

        String displaySymbol;
        BiPredicate<Integer, Integer> logic; //Predicate 逻辑

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
        return CommandHelper.executeReset(ctx, desc, entry -> {
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
        return CommandHelper.executeReset(ctx, desc,
                entry -> entry.getKey().distToCenterSqr(center.getX(), center.getY(), center.getZ()) <= rangeSqr);
    }

    public static int onResetByTime(CommandContext<CommandSourceStack> ctx) {
        String operator = StringArgumentType.getString(ctx, "operator");
        String durationStr = StringArgumentType.getString(ctx, "duration");

        long targetDuration;
        try {
            targetDuration = CommandHelper.parseDuration(durationStr);
        } catch (NumberFormatException e) {
            ctx.getSource().sendFailure(
                    Component.literal("时间格式错误: [")
                            .append(Component.literal(durationStr).withStyle(ChatFormatting.UNDERLINE))
                            .append("] (示例: 3d; 12h; 30m; 3d8h6m30s)")
            );
            return 0;
        }

        // 准备显示符号和逻辑
        String displaySymbol;
        BiPredicate<Long, Long> logic;

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

        return CommandHelper.executeReset(ctx, desc, entry -> {
            long registeredTime = entry.getValue().getRegisteredTime();

            if (registeredTime <= 0) return false;  // 非法数据

            // 计算由注册至今经过的时长 (Age)
            long age = now - registeredTime;

            return logic.test(age, targetDuration);
        });
    }

    public static int onResetByComplex(CommandContext<CommandSourceStack> ctx) {
        String query = StringArgumentType.getString(ctx, "query");
        try {
            // 1. 调用解析引擎生成逻辑
            Predicate<Map.Entry<BlockPos, LazyTickStatCache>> filter = FilterParser.parse(query);

            // 2. 构造描述文本
            Component desc = Component.literal("组合条件 [" + query + "]");

            // 3. 执行重置
            return CommandHelper.executeReset(ctx, desc, filter);

        } catch (CommandSyntaxException e) {
            // 捕获解析器throw出的语法错误并反馈
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    public static int onLimitSet(CommandContext<CommandSourceStack> ctx,
                                 Collection<GameProfile> profiles, int limit) {
        ServerLevel level = ctx.getSource().getLevel();
        LazyTickSavedLimitList data = LazyTickSavedLimitList.get(level);

        // 选择器可能返回@a(多个人的玩家档案,需要遍历)
        MutableComponent successMessage = Component.literal("已设置玩家 ");
        for (GameProfile profile : profiles) {
            data.setLimit(profile.getId(), limit);
            successMessage.append(Component.literal(profile.getName()).withStyle(ChatFormatting.GOLD)).append(" ");
        }
        successMessage.append("共" + profiles.size() + "人的懒惰刻调节配额为: ")
                .append(Component.literal(String.valueOf(limit)).withStyle(ChatFormatting.AQUA));
        ctx.getSource().sendSuccess(() -> successMessage, true);
        return profiles.size();
    }

    public static int onLimitRemove(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> profiles) {
        ServerLevel level = ctx.getSource().getLevel();
        LazyTickSavedLimitList data = LazyTickSavedLimitList.get(level);

        MutableComponent successMessage = Component.literal("已移除玩家 ");
        for (GameProfile profile : profiles) {
            data.removeLimit(profile.getId());
            successMessage.append(Component.literal(profile.getName()).withStyle(ChatFormatting.GOLD)).append(" ");
        }
        successMessage.append("共" + profiles.size() + "人的限制 (现在配额为无限)");

        ctx.getSource().sendSuccess(() -> successMessage, true);
        return profiles.size();
    }

    public static int onLimitCheck(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> profiles) {
        ServerLevel level = ctx.getSource().getLevel();
        LazyTickSavedLimitList limitData = LazyTickSavedLimitList.get(level);

        for (GameProfile profile : profiles) {
            // 1. 使用 UUID 获取限额
            int limit = limitData.getLimit(profile.getId());

            // 2. 获取当前用量 (注意: 目前还是用 Name 统计机器, (机器记录的是name,而不是uuid,后期需要重写get/setName为get/setOperatorUUID))
            // 否则玩家改名后可能无法操作其原来的机器
            int used = ForcedActiveManager.getPlayerUsageCount(level, profile.getName());

            MutableComponent limitDisplay = (limit == -1)
                    ? Component.literal("无限").withStyle(ChatFormatting.GREEN)
                    : Component.literal(String.valueOf(limit)).withStyle(ChatFormatting.AQUA);

            ChatFormatting statusColor = (limit != -1 && used >= limit) ? ChatFormatting.RED : ChatFormatting.GREEN;

            ctx.getSource().sendSuccess(() -> Component.literal("玩家 ")
                    .append(Component.literal(profile.getName()).withStyle(ChatFormatting.GOLD))
                    .append(" 状态统计:\n")
                    .append(" - 当前已激活: ").append(Component.literal(String.valueOf(used)).withStyle(statusColor)).append(" ")
                    .append(" - 最大配额: ").append(limitDisplay), false);
        }
        return profiles.size();
    }
}
