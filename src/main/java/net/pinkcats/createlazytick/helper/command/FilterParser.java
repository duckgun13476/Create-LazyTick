package net.pinkcats.createlazytick.helper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
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
    // 组1 (Key): 字母(允许引号包裹)
    // 组2 (Operator): 符号 (注意 >= 要在 > 前面)
    // 组3 (Value): 引号包裹的内容 或 不含空格连贯字符(且不含{})
    // (Key)(Op)(Val)
    public static final Pattern TOKEN_PATTERN = Pattern.compile("\"?([a-zA-Z]+)\"?(:|>=|<=|>|<|=)(\"[^\"]*\"|[^\"\\s,{}]+)");
    public static final Pattern TOKEN_PATTERN_FOR_SUGGESTION = Pattern.compile("^\"?([a-zA-Z]+)\"?(:|>=|<=|>|<|=)(.*)");

    private static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(
            Component.literal("未检测到筛选条件,格式示例: {name:mechanical_saw,value>50} (若括号内有空格,请在括号外再加一对双引号)"));

    public static Predicate<Map.Entry<BlockPos, LazyTickStatCache>> parse(String input) throws CommandSyntaxException {
        return parse(input, false);
    }

    //解析形如 'name:mechanical_saw value>50' 的字符串
    public static Predicate<Map.Entry<BlockPos, LazyTickStatCache>> parse(String input, boolean allowEmpty) throws CommandSyntaxException {
        if (input == null) input = "";

        // 剥离首尾大括号和引号 (支持 "{name:saw}" 写法)
        String trimmed = stripBracesAndQuotes(input);

        // 空检查
        if (trimmed.isBlank()) {
            if (allowEmpty) {
                // 允许空条件,意味着"全选/不筛选"(用于list)
                return entry -> true;
            } else {
                // 禁止空条件,防止误操作删库(用于reset等)
                throw ERROR_EMPTY.create();
            }
        }

        // 按空格或逗号分割成多个片段
        // "name:saw value>50, aaaaa" -> ["name:saw", "value>50", "aaaaa"]
        String[] segments = trimmed.split("[,\\s]+");

        // 初始逻辑为 true (全通过)
        Predicate<Map.Entry<BlockPos, LazyTickStatCache>> finalPredicate = entry -> true;
        boolean hasAnyValidFilter = false;

        for (String segment : segments) {
            // 跳过连续空格产生的空串
            if (segment.isBlank()) continue;

            // 如果片段本身被引号包裹,先剥引号(应对"val=50"这种整体片段)
            String cleanSegment = stripQuotes(segment);

            Matcher matcher = TOKEN_PATTERN.matcher(cleanSegment);

            // 检查每一段,如果不匹配正则,直接抛出异常
            if (!matcher.matches()) {
                // 不能直接new CommandSyntaxException(xxx),该异常接收的参数是受限的,需要手动构建后create
                throw new SimpleCommandExceptionType(
                        Component.literal("无法解析条件: [")
                                .append(Component.literal(cleanSegment).withStyle(ChatFormatting.UNDERLINE))
                                .append(Component.literal("] (格式应为 key:value)"))
                ).create();
            }

            // 提取数据 (此时已经确保格式正确)
            hasAnyValidFilter = true;
            String key = matcher.group(1).toLowerCase();
            String op = matcher.group(2);
            String rawVal = matcher.group(3);

            // 去除双引号
            String val = stripQuotes(rawVal);

            try {
                finalPredicate = finalPredicate.and(createSingleFilter(key, op, val));
            } catch (CommandSyntaxException e) {
                throw e;
            } catch (Exception e) {
                CreateLazyTick.LOGGER.error("An unexpected error occurred while trying to parse conditions!\n Key: {}, Val: {}", key, val, e);
                throw new SimpleCommandExceptionType(Component.literal("发生内部错误，请联系管理员检查后台日志")).create();
            }
        }

        if (!hasAnyValidFilter) {
            throw new SimpleCommandExceptionType(Component.literal("请输入有效的筛选条件 (示例: {name:saw,value>50} (若括号内有空格,请在括号外再加一对双引号))")).create();
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
                throw new SimpleCommandExceptionType(
                        Component.literal("模式参数错误: [")
                                .append(Component.literal(val).withStyle(ChatFormatting.UNDERLINE))
                                .append("] (仅限 forced 或 dynamic)")
                ).create();
            }
            case "value" -> {
                int targetVal;
                try {
                    targetVal = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    throw new SimpleCommandExceptionType(
                            Component.literal("数值格式错误: [")
                                    .append(Component.literal(val).withStyle(ChatFormatting.UNDERLINE))
                                    .append(Component.literal("]"))
                    ).create();
                }
                return entry -> compareInt(entry.getValue().getScrollValue(), targetVal, op);
            }
            case "time" -> {
                long targetDuration;
                try {
                    targetDuration = CommandHelper.parseDuration(val);
                } catch (NumberFormatException e) {
                    throw new SimpleCommandExceptionType(
                            Component.literal("时间格式错误: [")
                                    .append(Component.literal(val).withStyle(ChatFormatting.UNDERLINE))
                                    .append(Component.literal("]"))
                    ).create();
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
        throw new SimpleCommandExceptionType(
                Component.literal("未知的筛选键: [")
                        .append(Component.literal(key).withStyle(ChatFormatting.UNDERLINE))
                        .append(Component.literal("]"))
        ).create();
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

    public static String stripBracesAndQuotes(String input) {
        String s = input.trim();
        boolean changed = true;
        while (changed) {
            changed = false;
            // 剥引号
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length() - 1).trim();
                changed = true;
            }
            // 剥大括号
            if (s.length() >= 2 && s.startsWith("{") && s.endsWith("}")) {
                s = s.substring(1, s.length() - 1).trim();
                changed = true;
            }
        }
        return s;
    }

    private static String stripQuotes(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

}