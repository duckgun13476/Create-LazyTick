package net.pinkcats.createlazytick.helper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Feedback for clock interactions, which are handled on the dedicated server. */
public final class ClockModeFeedback {
    private ClockModeFeedback() {
    }

    public static List<Component> getDisplayComponents(int dynamicValue, int forcedValue, int maxTick) {
        boolean forced = forcedValue != -1;
        int percentage = forced ? forcedValue : dynamicValue;
        String modeKey;
        ChatFormatting color;

        if (forced) {
            if (forcedValue == 0) {
                modeKey = "createlazytick.mode.forced.full";
                color = ChatFormatting.DARK_PURPLE;
            } else if (forcedValue <= 30) {
                modeKey = "createlazytick.mode.forced.sleep_light";
                color = ChatFormatting.YELLOW;
            } else if (forcedValue <= 70) {
                modeKey = "createlazytick.mode.forced.sleep_medium";
                color = ChatFormatting.GOLD;
            } else {
                modeKey = "createlazytick.mode.forced.sleep_deep";
                color = ChatFormatting.RED;
            }
        } else if (dynamicValue > 0) {
            if (dynamicValue == 100) {
                modeKey = "createlazytick.mode.auto_sleep.default";
                color = ChatFormatting.DARK_GRAY;
            } else if (dynamicValue <= 30) {
                modeKey = "createlazytick.mode.auto_sleep.light";
                color = ChatFormatting.YELLOW;
            } else if (dynamicValue <= 70) {
                modeKey = "createlazytick.mode.auto_sleep.medium";
                color = ChatFormatting.GOLD;
            } else {
                modeKey = "createlazytick.mode.auto_sleep.deep";
                color = ChatFormatting.RED;
            }
        } else {
            return List.of(Component.translatable("createlazytick.mode.unknown")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        MutableComponent mode = Component.translatable(modeKey).withStyle(color);
        if (forced) {
            mode.withStyle(style -> style.withBold(true));
        }

        int interval = Math.max(1, maxTick * percentage / 100);
        MutableComponent extra = Component.translatable(
                forced ? "createlazytick.tooltip.extra.fixed" : "createlazytick.tooltip.extra.dynamic",
                percentage,
                interval + "t"
        ).withStyle(ChatFormatting.GRAY);
        return List.of(mode, Component.literal(" ").append(extra));
    }
}
