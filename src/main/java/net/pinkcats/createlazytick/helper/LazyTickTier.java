package net.pinkcats.createlazytick.helper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum LazyTickTier {
    ACTIVE("活跃", ChatFormatting.GREEN),       // <= 1
    LIGHT("轻度睡眠", ChatFormatting.YELLOW),   // 2 ~ 33% Max
    MEDIUM("中度睡眠", ChatFormatting.GOLD),    // 33% Max ~ Max
    DEEP("深度睡眠", ChatFormatting.RED);       // >= Max

    public final String name;
    public final ChatFormatting color;

    // 定义轻度睡眠的阈值比例 (例如 0.33 表示最大值的 1/3 算作轻度)
    private static final float LIGHT_THRESHOLD_RATIO = 0.33f;

    LazyTickTier(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    /**
     * 根据当前的延迟 tick 数，计算出它属于哪个档位。
     * 逻辑修改为动态比例，以适应不同机器配置的不同 maxTick。
     */
    public static LazyTickTier fromTicks(int currentTick, int maxTick) {
        if (currentTick <= 1) {
            return ACTIVE;
        }

        // 如果当前延迟已经达到或超过最大值，直接判定为深度睡眠
        if (currentTick >= maxTick) {
            return DEEP;
        }

        // 动态计算轻度睡眠的边界
        // 例如 max=60，threshold=20。 2~20 为 Light, 21~59 为 Medium
        int lightThreshold = (int) (maxTick * LIGHT_THRESHOLD_RATIO);

        // 保证阈值至少为 2，防止 maxTick 很小时出现逻辑错误
        if (lightThreshold < 2) lightThreshold = 2;

        if (currentTick <= lightThreshold) {
            return LIGHT;
        } else {
            return MEDIUM;
        }
    }

    // 获取显示文本
    // 优化：避免使用 String "+" 拼接，改用 Component 链式构建，减少内存垃圾
    public MutableComponent getDisplayComponent(int maxTick) {
        // 动态计算显示范围文本
        int lightThreshold = (int) (maxTick * LIGHT_THRESHOLD_RATIO);
        if (lightThreshold < 2) lightThreshold = 2;

        MutableComponent rangeText;

        if (this == ACTIVE) {
            rangeText = Component.literal("<= 1t");
        } else if (this == LIGHT) {
            rangeText = Component.literal("2-").append(String.valueOf(lightThreshold)).append("t");
        } else if (this == MEDIUM) {
            // 显示范围：(阈值+1) 到 (最大值-1)
            rangeText = Component.literal((lightThreshold + 1) + "-" + (maxTick - 1) + "t");
        } else { // DEEP
            rangeText = Component.literal(">=" + maxTick + "t");
        }

        // 构建最终组件: " [名称] (范围)"
        // 使用 append 避免创建大的 String 对象
        return Component.literal(" [")
                .append(Component.literal(this.name).withStyle(this.color))
                .append("] ")
                .append(Component.literal("(").withStyle(ChatFormatting.GRAY))
                .append(rangeText.withStyle(ChatFormatting.GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }
}