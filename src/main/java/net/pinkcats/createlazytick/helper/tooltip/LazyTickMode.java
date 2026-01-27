package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.pinkcats.createlazytick.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;

//需要翻译文本
public enum LazyTickMode {
    AUTO_SLEEP_LIGHT(" [浅度自动休眠模式]", ChatFormatting.YELLOW, false),
    AUTO_SLEEP_MEDIUM(" [中度自动休眠模式]", ChatFormatting.GOLD, false),
    AUTO_SLEEP_DEEP(" [深度自动休眠模式]", ChatFormatting.RED, false),
    AUTO_SLEEP_DEFAULT(" [深度自动休眠模式](默认)", ChatFormatting.DARK_GRAY, false),

    FORCED_FULL(" [强制全速模式]", ChatFormatting.DARK_PURPLE, true),
    FORCED_SLEEP_LIGHT( " [强制浅度休眠模式]", ChatFormatting.YELLOW, true),
    FORCED_SLEEP_MEDIUM( " [强制中度休眠模式]", ChatFormatting.GOLD, true),
    FORCED_SLEEP_DEEP( " [强制深度休眠模式]", ChatFormatting.RED, true);

    private final String text;
    private final ChatFormatting color;
    private final boolean isBold;
    LazyTickMode(String text, ChatFormatting color, boolean isBold) {
        this.text = text;
        this.color = color;
        this.isBold = isBold;
    }

    public static List<Component> getDisplayComponents(int dynamicValue, int forcedValue, int maxTick) {
        // 1. 决定当前模式
        LazyTickMode mode = resolveMode(dynamicValue, forcedValue);

        if (mode == null) {
            return List.of(Component.literal(" [未知模式]").withStyle(ChatFormatting.DARK_RED));
        }

        // 2. 生成额外数值说明信息
        MutableComponent extraInfo = generateExtraInfo(dynamicValue, forcedValue, maxTick);

        // 3. 根据配置渲染
        return renderByConfig(mode, extraInfo);
    }

    private static LazyTickMode resolveMode(int dynamicValue, int forcedValue) {
        // 优先判断强制模式
        if (forcedValue != -1) {
            if (forcedValue == 0) return FORCED_FULL;
            return getTierMode(forcedValue, true); // true 代表强制系列
        }

        // 判断动态模式
        if (dynamicValue > 0) {
            if (dynamicValue == 100) return AUTO_SLEEP_DEFAULT;
            return getTierMode(dynamicValue, false); // false 代表自动系列
        }

        return null; // 未知情况
    }

    //根据数值判断档位 (Light/Medium/Deep)
    private static LazyTickMode getTierMode(int value, boolean isForced) {
        if (value <= 30) return isForced ? FORCED_SLEEP_LIGHT : AUTO_SLEEP_LIGHT;
        if (value <= 70) return isForced ? FORCED_SLEEP_MEDIUM : AUTO_SLEEP_MEDIUM;
        return isForced ? FORCED_SLEEP_DEEP : AUTO_SLEEP_DEEP;
    }

    private static MutableComponent generateExtraInfo(int dynamicValue, int forcedValue, int maxTick) {
        boolean isForced = (forcedValue != -1);
        int percentage = isForced ? forcedValue : dynamicValue;

        // 1. 计算实际间隔时间
        int actualInterval = Math.max(1, maxTick * percentage / 100);

        // 2. 获取时间格式化字符串 (例如 "2.0s" 或 "1t")
        String timeStr = LazyTickTooltipTool.formatTime(actualInterval);

        // 3. 生成文本
        String text;
        if (isForced) {
            // [翻译键写法]:
            // return Component.translatable("tooltip.clt.extra.fixed", percentage, timeStr).withStyle(ChatFormatting.GRAY);

            // [中文写法]: (固定休眠: 0% | 1t)
            text = String.format("(固定休眠: %d%% | %s)", percentage, timeStr);
        } else {
            // return Component.translatable("tooltip.clt.extra.dynamic", percentage, timeStr).withStyle(ChatFormatting.GRAY);

            // (动态上限: 50% | 20t)
            text = String.format("(动态上限: %d%% | %s)", percentage, timeStr);
        }

        return Component.literal(text).withStyle(ChatFormatting.GRAY); // 统一设置为灰色
    }

    private MutableComponent getBaseComponent() {
        MutableComponent base = Component.literal(text);  // -> 比如 "[中度自动休眠模式]"
        base.withStyle(color);
        if (isBold) base.withStyle(style -> style.withBold(true));
        return base;
    }

    private static List<Component> renderByConfig(LazyTickMode mode, MutableComponent extraInfo) {
        List<Component> list = new ArrayList<>();

        // 获取配置
        ClientConfig.ModeFormat format = ClientConfig.getModeFormat();

        // 如果是 TEXT 或 BOTH，就添加第一行
        if (format == ClientConfig.ModeFormat.TEXT || format == ClientConfig.ModeFormat.BOTH) {
            list.add(mode.getBaseComponent());
        }

        // 如果是 NUMBER 或 BOTH，就添加第二行
        if (format == ClientConfig.ModeFormat.NUMBER || format == ClientConfig.ModeFormat.BOTH) {
            if (extraInfo != null) {
                list.add(Component.literal(" ").append(extraInfo));
            }
        }

        // 反空列表
        if (list.isEmpty()) {
            list.add(mode.getBaseComponent());
        }

        return list;
    }

    public static Component getModeDescription(int dynamicValue, int forcedValue) {
        // 强制模式解释
        if (forcedValue != -1) {
            if (forcedValue == 0) {
                return Component.literal("  说明: 懒加载优化已关闭，机器将保持全速运行。")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            return Component.literal("  说明: 忽略机器负载，强制按照固定的频率运行。")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }

        // 自动模式解释
        if (dynamicValue > 0) {
            if (dynamicValue == 100) {
                return Component.literal("  说明: 默认配置。机器将在不影响功能的前提下尽可能休眠。")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            return Component.literal("  说明: 根据负载动态计算休眠时长，但不会超过设定的上限。")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }

        // 预留文本
        return Component.literal("  说明: 如果你看到这个，可能哪里出了bug")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
    }
}