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

    public enum ModeFormat {
        TEXT,   // 仅显示文本: [Mode text]
        NUMBER, // 仅显示数字: (xx %|xx (t/s))
        BOTH    // 同时显示: (1st line)[text] (2nd line)number
    }

    public enum TierFormat {
        BAR,   // 仅显示状态条: [|||||···|||||]
        NUMBER, // 仅显示数字: (xx %|xx (t/s))
        BOTH    // 同时显示: (1st line)[bar] (2nd line)number
    }

    private static final ForgeConfigSpec.EnumValue<TimeFormat> TIME_FORMAT;
    private static final ForgeConfigSpec.EnumValue<ModeFormat> MODE_FORMAT;
    private static final ForgeConfigSpec.EnumValue<TierFormat> TIER_FORMAT;

    private static final ForgeConfigSpec.BooleanValue SHOW_MODE_TOOLTIP;
    private static final ForgeConfigSpec.BooleanValue SHOW_TIER_TOOLTIP;
    private static final ForgeConfigSpec.BooleanValue SHOW_DESCRIPTION_TOOLTIP;

    static {
        BUILDER.comment("Client-side Visual Settings").push("ui-visual");

        TIME_FORMAT = BUILDER
                .comment("How to display time in tooltips.",
                        "TICKS: Show game ticks (e.g. 60t)",
                        "SECONDS: Show seconds (e.g. 3.0s)",
                        "BOTH: Show both (e.g. 60t | 3.0s)")
                .defineEnum("time_format", TimeFormat.TICKS);

        MODE_FORMAT = BUILDER
                .comment("How to display mode in tooltips.",
                        "TEXT: Show mode text (e.g. [Deep Auto Sleep Mode])",
                        "NUMBER: Show limit number (e.g. dynamic:50% | 30t)",
                        "BOTH: Show both ((1st line)[mode-text] (2nd line)number)")
                .defineEnum("mode_format", ModeFormat.BOTH);

        TIER_FORMAT = BUILDER
                .comment("How to display tier in tooltips.",
                        "BAR: Show lazytick bar (e.g. [|||||···|||||])",
                        "NUMBER: Show current interval and config limited number (e.g. 1t / 60t)",
                        "BOTH: Show both ((1st line)[bar] (2nd line)number)")
                .defineEnum("tier_format", TierFormat.BOTH);

        SHOW_MODE_TOOLTIP = BUILDER
                .comment("Whether to show the mode of the Lazy Tick mode.")
                .define("show_mode_tooltip", true);

        SHOW_TIER_TOOLTIP = BUILDER
                .comment("Whether to show the tier of the Lazy Tick mode.")
                .define("show_tier_tooltip", true);

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
            System.out.println(e.getMessage());
            return TimeFormat.TICKS;
        }
    }

    public static ModeFormat getModeFormat() {
        try {
            return MODE_FORMAT.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ModeFormat.BOTH;
        }
    }

    public static TierFormat getTierFormat() {
        try {
            return TIER_FORMAT.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return TierFormat.BOTH;
        }
    }

    public static boolean showModeTooltip() {
        try {
            return SHOW_MODE_TOOLTIP.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean showTierTooltip() {
        try {
            return SHOW_TIER_TOOLTIP.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean showDescriptionTooltip() {
        try {
            return SHOW_DESCRIPTION_TOOLTIP.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}