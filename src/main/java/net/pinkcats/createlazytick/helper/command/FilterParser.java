package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.manager.LazyTickStatCache;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterParser {

    // 正则表达式循环匹配关键字组
    // 组1 (Key): 字母
    // 组2 (Operator): 符号 (注意 >= 要在 > 前面)
    // 组3 (Value): 引号包裹的内容 或 不含空格连贯字符
    // (Key)(Op)(Val)
    private static final Pattern TOKEN_PATTERN = Pattern.compile("([a-zA-Z]+)(:|>=|<=|>|<|=)(\"[^\"]*\"|[^\"\\s]+)");

    private static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(
            Component.literal("未检测到筛选条件，格式示例: name:mechanical_saw value>50"));

    //解析形如 'name:mechanical_saw value>50' 的字符串  //会扔俩exception,需要在上传前重新检查
    public static Predicate<Map.Entry<BlockPos, LazyTickStatCache>> parse(String input) throws CommandSyntaxException {
        if (input == null || input.isBlank()) {
            throw ERROR_EMPTY.create();
        }

        // 按空格分割成多个片段
        // "name:saw value>50 aaaaa" -> ["name:saw", "value>50", "aaaaa"]
        String[] segments = input.split("\\s+");

        // 初始逻辑为 true (全通过)
        Predicate<Map.Entry<BlockPos, LazyTickStatCache>> finalPredicate = entry -> true;
        boolean hasAnyValidFilter = false;

        for (String segment : segments) {
            // 跳过连续空格产生的空串
            if (segment.isBlank()) continue;

            Matcher matcher = TOKEN_PATTERN.matcher(segment);

            // 检查每一段,如果不匹配正则,直接抛出异常
            if (!matcher.matches()) {
                // 不能直接new CommandSyntaxException(xxx),该异常接收的参数是受限的,需要手动构建后create
                throw new SimpleCommandExceptionType(Component.literal("无法解析条件: \"" + segment + "\" (格式应为 key:value)")).create();
            }

            // 提取数据 (此时已经确保格式正确)
            hasAnyValidFilter = true;
            String key = matcher.group(1).toLowerCase();
            String op = matcher.group(2);
            String rawVal = matcher.group(3);

            // 去除双引号
            String val = rawVal.startsWith("\"") && rawVal.endsWith("\"")
                    ? rawVal.substring(1, rawVal.length() - 1)
                    : rawVal;

            try {
                finalPredicate = finalPredicate.and(createSingleFilter(key, op, val));
            } catch (CommandSyntaxException e) {
                throw e;
            } catch (Exception e) {
                CreateLazyTick.LOGGER.error("FilterParser 在试图解析条件时发生未预期的内部错误! Key: {}, Val: {}", key, val, e);
                throw new SimpleCommandExceptionType(Component.literal("发生内部错误，请联系管理员检查后台日志: " + e.getMessage())).create();
            }
        }

        if (!hasAnyValidFilter) {
            throw new SimpleCommandExceptionType(Component.literal("请输入有效的筛选条件 (示例: name:saw value>50)")).create();
        }

        return finalPredicate;
    }

    // 创建单个筛选条件
    private static Predicate<Map.Entry<BlockPos, LazyTickStatCache>> createSingleFilter(String key, String op, String val) throws CommandSyntaxException {
        switch (key) {
            case "name", "id" -> {
                return entry -> entry.getValue().getBlockName().toLowerCase().contains(val.toLowerCase());
            }
            case "operator", "player" -> {
                return entry -> entry.getValue().getOwnerName().equalsIgnoreCase(val);
            }
            case "mode" -> {
                if (val.equalsIgnoreCase("forced")) {
                    return entry -> entry.getValue().isForced();
                } else if (val.equalsIgnoreCase("dynamic")) {
                    return entry -> !entry.getValue().isForced();
                }
                throw new SimpleCommandExceptionType(Component.literal("模式参数错误: '" + val + "' (仅限 forced 或 dynamic)")).create();
            }
            case "value" -> {
                int targetVal;
                try {
                    targetVal = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    throw new SimpleCommandExceptionType(Component.literal("数值格式错误: " + val)).create();
                }
                return entry -> compareInt(entry.getValue().getScrollValue(), targetVal, op);
            }
            case "time" -> {
                long targetDuration;
                try {
                    targetDuration = CommandHelper.parseDuration(val);
                } catch (NumberFormatException e) {
                    throw new SimpleCommandExceptionType(Component.literal("时间格式错误: " + val)).create();
                }
                long now = System.currentTimeMillis();
                return entry -> {
                    long registeredTime = entry.getValue().getRegisteredTime();
                    if (registeredTime <= 0) return false;
                    long age = now - registeredTime;
                    return compareLong(age, targetDuration, op);
                };
            }
        }
        throw new SimpleCommandExceptionType(Component.literal("未知的筛选键: " + key)).create();
    }

    private static boolean compareInt(int a, int b, String op) {
        return switch (op) {
            case ">" -> a > b;
            case "<" -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            case "=", ":" -> a == b;
            default -> false;
        };
    }

    private static boolean compareLong(long a, long b, String op) {
        return switch (op) {
            // 对于"时间年龄(time/Age)"，大于(>)意味着"更老"，小于(<)意味着"更新"
            case ">" -> a > b;
            case "<" -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            case "=", ":" -> a == b;
            default -> false;
        };
    }

}