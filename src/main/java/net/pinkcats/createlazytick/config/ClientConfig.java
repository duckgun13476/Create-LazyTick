package net.pinkcats.createlazytick.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 时间显示格式枚举
    public enum TimeFormat {
        TICKS,   // 仅显示刻: 60t
        SECONDS, // 仅显示秒: 3.0s
        BOTH     // 同时显示: 60t | 3.0s
    }

    private static final ForgeConfigSpec.EnumValue<TimeFormat> TIME_FORMAT;
    private static final ForgeConfigSpec.BooleanValue SHOW_DESCRIPTION_TOOLTIP;

    static {
        BUILDER.comment("Client-side Visual Settings").push("visual");

        TIME_FORMAT = BUILDER
                .comment("How to display time in tooltips.",
                        "TICKS: Show game ticks (e.g. 60t)",
                        "SECONDS: Show seconds (e.g. 3.0s)",
                        "BOTH: Show both (e.g. 60t | 3.0s)")
                .defineEnum("time_format", TimeFormat.TICKS);

        SHOW_DESCRIPTION_TOOLTIP = BUILDER
                .comment("Whether to show the detailed description of the Lazy Tick mode.")
                .define("show_description_tooltip", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static TimeFormat getTimeFormat() {
        try {
            return TIME_FORMAT.get();
        } catch (Exception e) {
            return TimeFormat.TICKS;
        }
    }

    public static boolean showDescriptionTooltip() {
        try {
            return SHOW_DESCRIPTION_TOOLTIP.get();
        } catch (Exception e) {
            return false;
        }
    }
}