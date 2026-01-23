package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    public static Component getDisplayComponent(int dynamicValue, int forcedValue, int maxTick) {
        // 1. 优先判断强制模式 (只要不是 -1)
        if (forcedValue != -1) {
            if (forcedValue == 0) {
                // 强制全速 (Forced 0)
                return FORCED_FULL.getComponentWithExtra(null);
            } else {
                // 强制休眠 (Forced 1~100)
                LazyTickMode mode = forcedValue <= 30 ? FORCED_SLEEP_LIGHT :
                        forcedValue <= 70 ? FORCED_SLEEP_MEDIUM : FORCED_SLEEP_DEEP;

                int actualInterval = maxTick * forcedValue / 100;
                // 这里的 Math.max(1, ...) 是为了显示严谨，虽然显示0t也不影响理解
                actualInterval = Math.max(1, actualInterval);

                String extraInfo = " (固定休眠: " + forcedValue + "%" + "|" + actualInterval + "t" + ")";
                return mode.getComponentWithExtra(extraInfo);
            }
        }

        // 2. 动态模式
        // (理论上 Dynamic 不会是 -1，因为原子切换保证了互斥)
        if (dynamicValue > 0) {
            LazyTickMode mode;
            if (dynamicValue == 100) {
                mode = AUTO_SLEEP_DEFAULT;
            } else {
                mode = dynamicValue <= 30 ? AUTO_SLEEP_LIGHT :
                        dynamicValue <= 70 ? AUTO_SLEEP_MEDIUM : AUTO_SLEEP_DEEP;
            }

            int actualLimit = maxTick * dynamicValue / 100;
            actualLimit = Math.max(1, actualLimit);

            String extraInfo = " (动态上限: " + dynamicValue + "%" + "|" + actualLimit + "t" + ")";
            return mode.getComponentWithExtra(extraInfo);
        }
        return Component.literal(" [未知模式]").withStyle(ChatFormatting.GRAY);
    }

    private Component getComponentWithExtra(String extra) {
        MutableComponent base = Component.literal(text);
        if (extra != null) {
            base.append(Component.literal(extra));
        }

        base.withStyle(color);
        if (isBold) {
            base.withStyle(style -> style.withBold(true));
        }
        return base;
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