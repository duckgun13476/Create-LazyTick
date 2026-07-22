package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;

public enum LazyTickMode {
    AUTO_SLEEP_LIGHT("createlazytick.mode.auto_sleep.light", ChatFormatting.YELLOW, false),
    AUTO_SLEEP_MEDIUM("createlazytick.mode.auto_sleep.medium", ChatFormatting.GOLD, false),
    AUTO_SLEEP_DEEP("createlazytick.mode.auto_sleep.deep", ChatFormatting.RED, false),
    AUTO_SLEEP_DEFAULT("createlazytick.mode.auto_sleep.default", ChatFormatting.DARK_GRAY, false),

    FORCED_FULL("createlazytick.mode.forced.full", ChatFormatting.DARK_PURPLE, true),
    FORCED_SLEEP_LIGHT("createlazytick.mode.forced.sleep_light", ChatFormatting.YELLOW, true),
    FORCED_SLEEP_MEDIUM("createlazytick.mode.forced.sleep_medium", ChatFormatting.GOLD, true),
    FORCED_SLEEP_DEEP("createlazytick.mode.forced.sleep_deep", ChatFormatting.RED, true);

    private final String key;
    private final ChatFormatting color;
    private final boolean isBold;
    LazyTickMode(String key, ChatFormatting color, boolean isBold) {
        this.key = key;
        this.color = color;
        this.isBold = isBold;
    }

    public static List<Component> getDisplayComponents(int dynamicValue, int forcedValue, int maxTick) {
        // 1. 决定当前模式
        LazyTickMode mode = resolveMode(dynamicValue, forcedValue);

        if (mode == null) {
            return List.of(Component.translatable("createlazytick.mode.unknown").withStyle(ChatFormatting.DARK_RED));
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
        if (isForced) {
            return Component.translatable(
                    "createlazytick.tooltip.extra.fixed",
                    percentage,
                    timeStr
            ).withStyle(ChatFormatting.GRAY);
        } else {
            return Component.translatable(
                    "createlazytick.tooltip.extra.dynamic",
                    percentage,
                    timeStr
            ).withStyle(ChatFormatting.GRAY);

        }
    }

    private MutableComponent getBaseComponent() {
        MutableComponent base = Component.translatable(key); // ✅ 会走 lang
        base = base.withStyle(color);
        if (isBold) base = base.withStyle(style -> style.withBold(true));
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
                MutableComponent a = (MutableComponent) mes.spaces(1);
                list.add(a.append(extraInfo));
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
                return Component.translatable("createlazytick.mode.description.forced_full")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            return Component.translatable("createlazytick.mode.description.forced_sleep")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }

        // 自动模式解释
        if (dynamicValue > 0) {
            if (dynamicValue == 100) {
                return Component.translatable("createlazytick.mode.description.auto_default")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            return Component.translatable("createlazytick.mode.description.auto_dynamic")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }

        // 预留文本
        return Component.translatable("createlazytick.mode.description.unknown_bug")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
    }
}