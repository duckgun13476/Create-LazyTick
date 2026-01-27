package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

//需要秒和tick(转换)
public enum LazyTickTier {
    ACTIVE(ChatFormatting.GREEN),
    LIGHT(ChatFormatting.YELLOW),
    MEDIUM(ChatFormatting.GOLD),
    DEEP(ChatFormatting.RED);

    public final ChatFormatting color;

    // 总格数 34
    // 逻辑：前 33 格每格代表 3% (0~99%)，第 34 格代表最后 1% (100%)
    private static final int BAR_COUNT = 34;

    LazyTickTier(ChatFormatting color) {
        this.color = color;
    }

    /**
     * @param currentInterval 当前实际休眠刻数 (动态变化的延迟)
     * @param maxTick 配置最大刻数
     * @param limitPercent 玩家设定的上限百分比 (0-100)
     */
    public List<MutableComponent> getAdvancedProgressBar(int currentInterval, int maxTick, int limitPercent) {
        if (maxTick < 1) maxTick = 1;

        // 1. 活跃机器判定 (配置上限极低的机器)
        // 如果 maxTick <= 2，说明机器本身就没有多少"懒惰"的空间，总是视为活跃
        boolean isActiveMachine = maxTick <= 2;

        // 2. 低负载判定 (新增逻辑)
        // 只要当前延迟 <= 3t (约 0.15秒)，无论占比多少，都视为"绿色/低负载"
        boolean isLowLoad = currentInterval <= 3;

        // 3. 计算填充格数
        int filledBars;

        if (isActiveMachine) {
            // 活跃机器强制显示 1 格
            filledBars = 1;
        } else {
            // 计算百分比
            float percent = (float) currentInterval / maxTick * 100f;

            if (percent >= 99.1f) {
                filledBars = 34; // >99% 填满
            } else {
                filledBars = (int) Math.ceil(percent / 3.0f); // 0-99% 每 3% 一格
            }

            // 边界修正
            if (filledBars > BAR_COUNT) filledBars = BAR_COUNT;
            // 只要有延迟，至少亮一格
            if (filledBars == 0 && currentInterval > 0) filledBars = 1;
        }

        // 4. 计算紫色游标位置
        int limitIndex;
        if (limitPercent >= 100) {
            limitIndex = 33;
        } else if (limitPercent <= 0) {
            limitIndex = 0;
        } else {
            limitIndex = limitPercent / 3;
        }

        MutableComponent bar = Component.literal(" [").withStyle(ChatFormatting.GRAY);

        // 5. 循环渲染
        for (int i = 0; i < BAR_COUNT; i++) {
            ChatFormatting barColor;

            // 优先级 A: 紫色游标
            if (i == limitIndex) {
                barColor = ChatFormatting.DARK_PURPLE;
            }
            // 优先级 B: 进度条填充部分
            else if (i < filledBars) {
                if (isActiveMachine || isLowLoad) {
                    // [修改点] 活跃机器 或 低负载(<=3t) -> 强制绿色
                    barColor = ChatFormatting.GREEN;
                } else {
                    // [修改点] 其他情况走百分比渐变
                    float progress = (float) i / BAR_COUNT;
                    // 这里去掉了之前的 <0.15f 判断，因为已经被 isLowLoad 接管了
                    // 直接从黄色开始过渡颜色，视觉上更清晰
                    if (progress < 0.40f) {
                        barColor = ChatFormatting.YELLOW; // < 40%
                    } else if (progress < 0.70f) {
                        barColor = ChatFormatting.GOLD;   // 40% - 70%
                    } else {
                        barColor = ChatFormatting.RED;    // > 70%
                    }
                }
            }
            // 优先级 C: 背景
            else {
                barColor = ChatFormatting.DARK_GRAY;
            }

            bar.append(Component.literal("|").withStyle(barColor));
        }

        // 6. 尾部数值
        bar.append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

        String currStr = LazyTickTooltipHelper.formatTime(currentInterval);
        String maxStr = LazyTickTooltipHelper.formatTime(maxTick);

        String timeText = String.format("(%s / %s)", currStr, maxStr);

        MutableComponent statsLine = Component.literal("  " + timeText)
                .withStyle(ChatFormatting.GRAY);

        return List.of(bar, statsLine);
    }

    /**
     * 根据当前的延迟 tick 数，计算出它属于哪个档位。
     * 逻辑修改为动态比例，以适应不同机器配置的不同 maxTick。
     */
    public static LazyTickTier fromTicks(int currentInterval, int maxTick) {
        if (maxTick < 1) maxTick = 1;

        if (maxTick <= 2) return ACTIVE;

        // 2. 低负载 (延迟很低) -> 总是 Active (对应绿色)
        if (currentInterval <= 3) return ACTIVE;

        // 如果当前延迟已经达到或超过最大值，直接判定为深度睡眠
        if (currentInterval >= maxTick) {
            return DEEP;
        }

        float percent = (float) currentInterval / maxTick;

        if (percent < 0.40f) return LIGHT;   // < 40% Yellow
        if (percent < 0.70f) return MEDIUM;  // 40-70% Gold
        return DEEP;
    }
}