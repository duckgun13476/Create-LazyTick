package net.pinkcats.createlazytick.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ==========================================
    // ServerConfig Spec Objects (Private)
    // ==========================================

    // General
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_TICK;
    private static final ForgeConfigSpec.IntValue GLOBAL_CACHE_RECORD_DELAY;

    // Fluids
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FLUID;
    private static final ForgeConfigSpec.IntValue FLUID_DELAY_MAX;

    // Logistics (Funnel, Chute, Belt)
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FUNNEL;
    private static final ForgeConfigSpec.IntValue FUNNEL_DELAY_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CHUTE;
    private static final ForgeConfigSpec.IntValue CHUTE_DELAY_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BELT;
    private static final ForgeConfigSpec.IntValue BELT_DELAY_MAX;

    // Processing (Depot, Saw, Basin, Item Drain, Deployer, Spout)
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_DEPOT;
    private static final ForgeConfigSpec.IntValue DEPOT_DELAY_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_SAW;
    private static final ForgeConfigSpec.IntValue SAW_DELAY_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_SAW;
    private static final ForgeConfigSpec.IntValue SAW_CACHE_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BASIN;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_BASIN_RELATED_RECIPE_TYPES;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ITEM_DRAIN;
    private static final ForgeConfigSpec.IntValue ITEM_DRAIN_DELAY_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_DEPLOYER;
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_SPOUT;
    private static final ForgeConfigSpec.IntValue SPOUT_CACHE_MAX;

    // Crafter
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_CRAFTER;
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_CRAFTER_DEBUGGER;
    private static final ForgeConfigSpec.IntValue CRAFTER_GLOBAL_CACHE_MAX;
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CRAFTER_REDSTONE;
    private static final ForgeConfigSpec.IntValue CRAFTER_REDSTONE_DELAY_MAX;

    // Mechanical Arm
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ARM;
    private static final ForgeConfigSpec.IntValue ARM_DELAY_MAX;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARM_IGNORE_LAZYTICK_LIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARM_WEAK_LAZYTICK_LIST;
    private static final ForgeConfigSpec.IntValue ARM_WEAK_DELAY_MAX;

    // LazyTick-Clock
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CLOCK_MODE_SEQUENCE;
    private static final ForgeConfigSpec.BooleanValue CLOCK_MODE_DEFAULT_DYNAMIC;

    // ==========================================
    // Initialization Block
    // ==========================================
    static {
        // --- General Settings ---
        BUILDER.comment("Global Settings").push("general");

        ENABLE_LAZY_TICK = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable lazy tick mixin")
                .define("enable_lazy_tick", true);

        GLOBAL_CACHE_RECORD_DELAY = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("""
                        Delay time (in seconds) for global cache to start recording again after a server reload.
                        This setting prevents abnormal recipes from being recorded in the global cache due to unstable recipes after a reload.
                        Warning: A lower value carries higher risks. Please consider carefully before changing this setting.""")
                .defineInRange("global_cache_record_delay", 40, 20, 600);

        BUILDER.pop();

        // --- Fluid Settings ---
        BUILDER.comment("Fluid System Settings").push("fluids");

        ENABLE_LAZY_FLUID = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable fluid pipe lazy tick")
                .define("enable_lazy_fluid", true);

        FLUID_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Delay ticks for fluid system.(Large number is not recommended.)")
                .defineInRange("Fluid_delay_max", 5, 1, 10);

        BUILDER.pop();

        // --- Logistics Settings (Funnels, Chutes, Belts) ---
        BUILDER.comment("Logistics Blocks Settings").push("logistics");

        // Funnel
        ENABLE_LAZY_FUNNEL = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable funnel lazy tick")
                .define("enable_lazy_funnel", true);
        FUNNEL_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if funnel is rest")
                .defineInRange("funnel_delay_max", 60, 20, Integer.MAX_VALUE);

        // Chute
        ENABLE_LAZY_CHUTE = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable chute lazy tick")
                .define("enable_lazy_chute", true);
        CHUTE_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if chute is rest")
                .defineInRange("chute_delay_max", 60, 20, Integer.MAX_VALUE);

        // Belt
        ENABLE_LAZY_BELT = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable belt lazy tick")
                .define("enable_lazy_belt", true);
        BELT_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if belt is rest")
                .defineInRange("belt_delay_max", 60, 20, Integer.MAX_VALUE);

        BUILDER.pop();

        // --- Processing Settings (Depot, Saw, Basin, Item Drain, Deployer, Spout) ---
        BUILDER.comment("Processing Blocks Settings").push("processing");

        // Depot
        ENABLE_LAZY_DEPOT = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable depot lazy tick")
                .define("enable_lazy_depot", true);
        DEPOT_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if depot is rest")
                .defineInRange("depot_delay_max", 60, 20, Integer.MAX_VALUE);

        // Saw
        ENABLE_LAZY_SAW = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable saw lazy tick")
                .define("enable_lazy_saw", true);
        SAW_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if saw is rest")
                .defineInRange("saw_delay_max", 60, 20, Integer.MAX_VALUE);
        ENABLE_CACHE_SAW = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable saw cache to improve efficiency of finding recipes")
                .define("enable_cache_saw", true);
        SAW_CACHE_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max cache count for each saw")
                .defineInRange("saw_cache_max", 60, 1, Integer.MAX_VALUE);

        // Basin
        ENABLE_LAZY_BASIN = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable basin lazy tick by cached recipes(Influence on mechanical mixer and mechanical press)")
                .define("enable_lazy_basin", true);

        EXTRA_BASIN_RELATED_RECIPE_TYPES = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Additional recipe type resource locations to include in the basin recipe index.",
                        "These types will be considered in addition to the default types (BASIN, MIXING, COMPACTING, PRESSING).",
                        "Example: [\"create:cutting\", \"othermod:mixing\"]")
                .defineList("extra_basin_related_recipe_types", java.util.Collections.emptyList(), o -> o instanceof String);

        // Item Drain
        ENABLE_LAZY_ITEM_DRAIN = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable item drain lazy tick")
                .define("enable_lazy_item_drain", true);
        ITEM_DRAIN_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if something stuck on the item drain")
                .defineInRange("item_drain_delay_max", 60, 20, Integer.MAX_VALUE);

        // Deployer
        ENABLE_CACHE_DEPLOYER = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable deployer cache to improve efficiency")
                .define("enable_cache_deployer", true);

        // Spout
        ENABLE_CACHE_SPOUT = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable global spout cache.")
                .define("enable_cache_spout", true);
        SPOUT_CACHE_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max cache count for global spout cache")
                .defineInRange("spout_cache_max", 500, 1, Integer.MAX_VALUE);


        BUILDER.pop();

        // --- Crafter Settings ---
        BUILDER.comment("Crafter Settings").push("crafter");

        ENABLE_CACHE_CRAFTER = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable crafter cache to improve efficiency of finding recipes")
                .define("enable_cache_crafter", true);
        ENABLE_CACHE_CRAFTER_DEBUGGER = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable crafter debugger to show cache stats.")
                .define("enable_cache_crafter_debugger", false);
        CRAFTER_GLOBAL_CACHE_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max cache count for global crafter")
                .defineInRange("crafter_global_cache_max", 500, 10, Integer.MAX_VALUE);
        ENABLE_LAZY_CRAFTER_REDSTONE = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("""
                        Whether to enable crafter redstone lazy tick.
                        Note: When this option is enabled, if you attempt to activate a mechanical crafter in a lazy-loaded state via redstone,
                        you must ensure that the duration of the changed redstone signal is longer than the lazy-loading interval.
                        A mechanical crafter in an active state can respond immediately without requiring the above conditions.""")
                .define("enable_lazy_crafter_redstone", true);
        CRAFTER_REDSTONE_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("The maximum delay for activating a mechanical crafter via redstone after it has been inactive for a period of time.")
                .defineInRange("crafter_redstone_delay_max", 60, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        // --- Mechanical Arm Settings ---
        BUILDER.comment("Mechanical Arm Settings").push("arm");

        ENABLE_LAZY_ARM = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("Whether to enable mechanical arm lazy tick")
                .define("enable_lazy_arm", true);
        ARM_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("max delay tick if arm is searching for items/targets")
                .defineInRange("arm_delay_max", 60, 1, Integer.MAX_VALUE);

        ARM_IGNORE_LAZYTICK_LIST = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("List of blocks that force the arm to work at full speed (Disable Lazy Tick).",
                        "Use this for fast-moving inputs like belts.")
                .defineList("arm_ignore_lazytick_list",
                        java.util.List.of("create:belt"), // 默认传送带全速
                        o -> o instanceof String);

        ARM_WEAK_LAZYTICK_LIST = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("List of blocks that allow lazy ticking but with a reduced max delay (Weak Lazy).",
                        "Use this for time-sensitive outputs like blaze burners.")
                .defineList("arm_weak_lazytick_list",
                        java.util.List.of("create:blaze_burner"), // 默认燃烧室浅睡眠
                        o -> o instanceof String);

        ARM_WEAK_DELAY_MAX = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment("The max delay tick for blocks in the 'Weak Lazy' list.")
                .defineInRange("arm_weak_delay_max", 10, 1, Integer.MAX_VALUE);

        BUILDER.pop();

        BUILDER.comment("Lazytick-Clock Settings").push("lazytick-clock");

        CLOCK_MODE_SEQUENCE = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment(" [Clock Item] Right-click Cycle Sequence")
                .comment(" Defines the sequence of values cycled through when right-clicking a machine with the Clock Item.")
                .comment(" Only integers between 0 and 100 are allowed.")
                .comment("   0   = Disable Optimization (Run at full speed)")
                .comment("   100 = Maximum Effect (Represents Max Dynamic Limit % or Max Forced Interval %, depending on the mode)")
                .comment(" Default: [0, 25, 50, 75, 100]")
                .defineList("clock_mode_sequence",
                        List.of(0, 25, 50, 75, 100),
                        obj -> obj instanceof Integer && (Integer) obj >= 0 && (Integer) obj <= 100);

        CLOCK_MODE_DEFAULT_DYNAMIC = BUILDER
                .comment("")
                .comment("--------------------------------------------------------------------------")
                .comment(" [Clock Item] Default Mode Preference")
                .comment(" Determines which mode the clock item will adjust when right-clicking.")
                .comment("   true  = Dynamic Mode (Adjusts the dynamic optimization limit %)")
                .comment("   false = Forced Mode (Adjusts the fixed sleep interval %)")
                .define("clock_mode_default_dynamic", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // ==========================================
    // Public Access Methods
    // ==========================================

    public static boolean getEnableLazyTick() {
        return ENABLE_LAZY_TICK.get();
    }

    public static int getGlobalCacheRecordDelay() {
        return GLOBAL_CACHE_RECORD_DELAY.get();
    }

    public static boolean getEnableLazyFluid() {
        return ENABLE_LAZY_FLUID.get();
    }

    public static int getFluidDelayMax() {
        return FLUID_DELAY_MAX.get();
    }

    public static boolean getEnableLazyFunnel() {
        return ENABLE_LAZY_FUNNEL.get();
    }

    public static int getFunnelDelayMax() {
        return FUNNEL_DELAY_MAX.get();
    }

    public static boolean getEnableLazyChute() {
        return ENABLE_LAZY_CHUTE.get();
    }

    public static int getChuteDelayMax() {
        return CHUTE_DELAY_MAX.get();
    }

    public static boolean getEnableLazyBelt() {
        return ENABLE_LAZY_BELT.get();
    }

    public static int getBeltDelayMax() {
        return BELT_DELAY_MAX.get();
    }

    public static boolean getEnableLazyDepot() {
        return ENABLE_LAZY_DEPOT.get();
    }

    public static int getDepotDelayMax() {
        return DEPOT_DELAY_MAX.get();
    }

    public static boolean getEnableLazySaw() {
        return ENABLE_LAZY_SAW.get();
    }

    public static int getSawDelayMax() {
        return SAW_DELAY_MAX.get();
    }

    public static boolean getEnableCacheSaw() {
        return ENABLE_CACHE_SAW.get();
    }

    public static int getSawCacheMax() {
        return SAW_CACHE_MAX.get();
    }

    public static boolean getEnableLazyBasin() {
        return ENABLE_LAZY_BASIN.get();
    }

    public static List<? extends String> getExtraBasinRelatedRecipeTypes() {
        return EXTRA_BASIN_RELATED_RECIPE_TYPES.get();
    }

    public static boolean getEnableLazyItemDrain() {
        return ENABLE_LAZY_ITEM_DRAIN.get();
    }

    public static int getItemDrainDelayMax() {
        return ITEM_DRAIN_DELAY_MAX.get();
    }

    public static boolean getEnableCacheDeployer() {
        return ENABLE_CACHE_DEPLOYER.get();
    }

    public static boolean getEnableCacheSpout() {
        return ENABLE_CACHE_SPOUT.get();
    }

    public static int getSpoutCacheMax() {
        return SPOUT_CACHE_MAX.get();
    }

    public static boolean getEnableCacheCrafter() {
        return ENABLE_CACHE_CRAFTER.get();
    }

    public static boolean getEnableCacheCrafterDebugger() {
        return ENABLE_CACHE_CRAFTER_DEBUGGER.get();
    }

    public static int getCrafterGlobalCacheMax() {
        return CRAFTER_GLOBAL_CACHE_MAX.get();
    }

    public static boolean getEnableLazyCrafterRedstone() {
        return ENABLE_LAZY_CRAFTER_REDSTONE.get();
    }

    public static int getCrafterRedstoneDelayMax() {
        return CRAFTER_REDSTONE_DELAY_MAX.get();
    }

    public static boolean getEnableLazyArm() {
        return ENABLE_LAZY_ARM.get();
    }

    public static int getArmDelayMax() {
        return ARM_DELAY_MAX.get();
    }

    public static List<? extends String> getArmIgnoreLazytickList() {
        return ARM_IGNORE_LAZYTICK_LIST.get();
    }

    public static List<? extends String> getArmWeakLazytickList() {
        return ARM_WEAK_LAZYTICK_LIST.get();
    }

    public static int getArmWeakDelayMax() {
        return ARM_WEAK_DELAY_MAX.get();
    }

    public static List<? extends Integer> getClockModeSequence() {
        return CLOCK_MODE_SEQUENCE.get();
    }

    public static boolean getClockModeDefaultDynamic() {
        return CLOCK_MODE_DEFAULT_DYNAMIC.get();
    }
}