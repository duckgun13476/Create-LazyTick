package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum LazyTickMode {
    AUTO(0, " [自动休眠模式]", ChatFormatting.GRAY),
    FORCED_FULL(1, " [强制全速模式]", ChatFormatting.DARK_PURPLE),
    FORCED_SLEEP_LIGHT(2, " [强制浅度休眠模式]", ChatFormatting.YELLOW),
    FORCED_SLEEP_MEDIUM(3, " [强制中度休眠模式]", ChatFormatting.GOLD),
    FORCED_SLEEP_DEEP(4, " [强制深度休眠模式]", ChatFormatting.RED);

    private final int id;
    private final Component displayComponent;

    LazyTickMode(int id, String text, ChatFormatting color) {
        this.id = id;
        this.displayComponent = Component.literal(text).withStyle(color);
    }

    public static LazyTickMode fromId(int id) {
        for (LazyTickMode state : values()) {
            if (state.id == id) return state;
        }
        return AUTO; // 默认 fallback
    }

    public Component getDisplayComponent() {
        return displayComponent;
    }
}