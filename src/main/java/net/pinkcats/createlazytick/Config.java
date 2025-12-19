package net.pinkcats.createlazytick;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = CreateLazyTick.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 声明 public static final 并在 static 块中赋值,以获得更好的分割效果

    // ==========================================
    // Config Spec Objects
    // ==========================================

    // General
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_TICK;

    // Fluids
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FLUID;

    // Logistics (Funnel, Chute, Belt)
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FUNNEL;
    public static final ForgeConfigSpec.IntValue FUNNEL_DELAY_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CHUTE;
    public static final ForgeConfigSpec.IntValue CHUTE_DELAY_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BELT;
    public static final ForgeConfigSpec.IntValue BELT_DELAY_MAX;

    // Processing (Depot, Saw, Basin, Item Drain, Deployer)
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_DEPOT;
    public static final ForgeConfigSpec.IntValue DEPOT_DELAY_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_SAW;
    public static final ForgeConfigSpec.IntValue SAW_CACHE_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BASIN;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ITEM_DRAIN;
    public static final ForgeConfigSpec.IntValue ITEM_DRAIN_DELAY_MAX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_DEPLOYER;

    // Crafter
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_CRAFTER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_CRAFTER_DEBUGGER;
    public static final ForgeConfigSpec.IntValue CRAFTER_GLOBAL_CACHE_MAX;
    public static final ForgeConfigSpec.IntValue CRAFTER_CACHE_RECORD_DELAY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CRAFTER_REDSTONE;
    public static final ForgeConfigSpec.IntValue CRAFTER_REDSTONE_DELAY_MAX;

    // Mechanical Arm
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ARM;
    public static final ForgeConfigSpec.IntValue ARM_DELAY_MAX;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARM_IGNORE_LAZYTICK_LIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARM_WEAK_LAZYTICK_LIST;
    public static final ForgeConfigSpec.IntValue ARM_WEAK_DELAY_MAX;

    static final ForgeConfigSpec SPEC;

    // ==========================================
    // Initialization Block (Organizing Categories)
    // ==========================================
    static {
        // --- General Settings ---
        BUILDER.comment("Global Settings").push("general");

        ENABLE_LAZY_TICK = BUILDER
                .comment("Whether to enable lazy tick mixin")
                .define("enable_lazy_tick", true);

        BUILDER.pop();

        // --- Fluid Settings ---
        BUILDER.comment("Fluid Pipe Settings").push("fluids");

        ENABLE_LAZY_FLUID = BUILDER
                .comment("Whether to enable fluid pipe lazy tick")
                .define("enable_lazy_fluid", true);

        BUILDER.pop();

        // --- Logistics Settings (Funnels, Chutes, Belts) ---
        BUILDER.comment("Logistics Blocks Settings").push("logistics");

        // Funnel
        ENABLE_LAZY_FUNNEL = BUILDER
                .comment("Whether to enable funnel lazy tick")
                .define("enable_lazy_funnel", true);
        FUNNEL_DELAY_MAX = BUILDER
                .comment("max delay tick if funnel is rest")
                .defineInRange("funnel_delay_max", 120, 20, Integer.MAX_VALUE);

        // Chute
        ENABLE_LAZY_CHUTE = BUILDER
                .comment("Whether to enable chute lazy tick")
                .define("enable_lazy_chute", true);
        CHUTE_DELAY_MAX = BUILDER
                .comment("max delay tick if chute is rest")
                .defineInRange("chute_delay_max", 60, 20, Integer.MAX_VALUE);

        // Belt
        ENABLE_LAZY_BELT = BUILDER
                .comment("Whether to enable belt lazy tick")
                .define("enable_lazy_belt", true);
        BELT_DELAY_MAX = BUILDER
                .comment("max delay tick if belt is rest")
                .defineInRange("belt_delay_max", 60, 20, Integer.MAX_VALUE);

        BUILDER.pop();

        // --- Processing Settings (Depot, Saw, Basin, Item Drain, Deployer) ---
        BUILDER.comment("Processing Blocks Settings").push("processing");

        // Depot
        ENABLE_LAZY_DEPOT = BUILDER
                .comment("Whether to enable depot lazy tick")
                .define("enable_lazy_depot", true);
        DEPOT_DELAY_MAX = BUILDER
                .comment("max delay tick if depot is rest")
                .defineInRange("depot_delay_max", 60, 20, Integer.MAX_VALUE);

        // Saw
        ENABLE_CACHE_SAW = BUILDER
                .comment("Whether to enable saw cache to improve efficiency of finding recipes")
                .define("enable_cache_saw", true);
        SAW_CACHE_MAX = BUILDER
                .comment("max cache count for each saw")
                .defineInRange("saw_cache_max", 50, 1, Integer.MAX_VALUE);

        // Basin
        ENABLE_LAZY_BASIN = BUILDER
                .comment("Whether to enable basin lazy tick by cached recipes(Influence on mechanical mixer and mechanical press)")
                .define("enable_lazy_basin", true);

        // Item Drain
        ENABLE_LAZY_ITEM_DRAIN = BUILDER
                .comment("Whether to enable item drain lazy tick")
                .define("enable_lazy_item_drain", true);
        ITEM_DRAIN_DELAY_MAX = BUILDER
                .comment("max delay tick if something stuck on the item drain")
                .defineInRange("item_drain_delay_max", 60, 20, Integer.MAX_VALUE);

        // Deployer
        ENABLE_CACHE_DEPLOYER = BUILDER
                .comment("Whether to enable deployer cache to improve efficiency")
                .define("enable_cache_deployer", true);

        BUILDER.pop();

        // --- Crafter Settings ---
        BUILDER.comment("Mechanical Crafter Settings").push("crafter");

        ENABLE_CACHE_CRAFTER = BUILDER
                .comment("Whether to enable crafter cache to improve efficiency of finding recipes")
                .define("enable_cache_crafter", true);
        ENABLE_CACHE_CRAFTER_DEBUGGER = BUILDER
                .comment("Whether to enable crafter debugger to show cache stats.")
                .define("enable_cache_crafter_debugger", false);
        CRAFTER_GLOBAL_CACHE_MAX = BUILDER
                .comment("max cache count for global crafter")
                .defineInRange("crafter_global_cache_max", 500, 10, Integer.MAX_VALUE);
        CRAFTER_CACHE_RECORD_DELAY = BUILDER
                .comment("""
                        Delay time (in seconds) for crafter cache to start recording again after a server reload.
                        This setting prevents abnormal recipes from being recorded in the global cache due to unstable recipes after a reload.
                        Warning: A lower value carries higher risks. Please consider carefully before changing this setting.""")
                .defineInRange("crafter_cache_record_delay", 40, 0, 600);
        ENABLE_LAZY_CRAFTER_REDSTONE = BUILDER
                .comment("""
                        Whether to enable crafter redstone lazy tick.
                        Note: When this option is enabled, if you attempt to activate a mechanical crafter in a lazy-loaded state via redstone,
                        you must ensure that the duration of the changed redstone signal is longer than the lazy-loading interval.
                        A mechanical crafter in an active state can respond immediately without requiring the above conditions.""")
                .define("enable_lazy_crafter_redstone", true);
        CRAFTER_REDSTONE_DELAY_MAX = BUILDER
                .comment("The maximum delay for activating a mechanical crafter via redstone after it has been inactive for a period of time.")
                .defineInRange("crafter_redstone_delay_max", 60, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        // --- Mechanical Arm Settings ---
        BUILDER.comment("Mechanical Arm Settings").push("arm");

        ENABLE_LAZY_ARM = BUILDER
                .comment("Whether to enable mechanical arm lazy tick")
                .define("enable_lazy_arm", true);
        ARM_DELAY_MAX = BUILDER
                .comment("max delay tick if arm is searching for items/targets")
                .defineInRange("arm_delay_max", 60, 1, Integer.MAX_VALUE);

        ARM_IGNORE_LAZYTICK_LIST = BUILDER
                .comment("List of blocks that force the arm to work at full speed (Disable Lazy Tick).",
                        "Use this for fast-moving inputs like belts.")
                .defineList("arm_ignore_lazytick_list",
                        java.util.List.of("create:belt"), // 默认传送带全速
                        o -> o instanceof String);

        ARM_WEAK_LAZYTICK_LIST = BUILDER
                .comment("List of blocks that allow lazy ticking but with a reduced max delay (Weak Lazy).",
                        "Use this for time-sensitive outputs like blaze burners.")
                .defineList("arm_weak_lazytick_list",
                        java.util.List.of("create:blaze_burner"), // 默认燃烧室浅睡眠
                        o -> o instanceof String);

        ARM_WEAK_DELAY_MAX = BUILDER
                .comment("The max delay tick for blocks in the 'Weak Lazy' list.")
                .defineInRange("arm_weak_delay_max", 10, 1, Integer.MAX_VALUE);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // ==========================================
    // Public Access Fields
    // ==========================================

    public static boolean enable_lazy_tick;

    public static boolean enable_lazy_fluid;
    public static boolean enable_lazy_funnel;
    public static boolean enable_lazy_chute;
    public static boolean enable_lazy_depot;
    public static boolean enable_cache_saw;
    public static boolean enable_belt_delay;
    public static boolean enable_lazy_basin;
    public static boolean enable_lazy_item_drain;
    public static boolean enable_cache_crafter;
    public static boolean enable_cache_crafter_debugger;
    public static boolean enable_lazy_crafter_redstone;
    public static boolean enable_lazy_arm;
    public static boolean enable_cache_deployer;

    public static List<? extends String> arm_ignore_lazytick_list;
    public static List<? extends String> arm_weak_lazytick_list;

    public static int funnel_delay_max;
    public static int chute_delay_max;
    public static int depot_delay_max;
    public static int saw_cache_max;
    public static int belt_delay_max;
    public static int item_drain_delay_max;
    public static int crafter_global_cache_max;
    public static int crafter_cache_record_delay;
    public static int crafter_redstone_delay_max;
    public static int arm_weak_delay_max;
    public static int arm_delay_max;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enable_lazy_tick = ENABLE_LAZY_TICK.get();

        enable_lazy_fluid = ENABLE_LAZY_FLUID.get();
        enable_lazy_funnel = ENABLE_LAZY_FUNNEL.get();
        enable_lazy_chute = ENABLE_LAZY_CHUTE.get();
        enable_lazy_depot = ENABLE_LAZY_DEPOT.get();
        enable_cache_saw = ENABLE_CACHE_SAW.get();
        enable_belt_delay = ENABLE_LAZY_BELT.get();
        enable_lazy_basin = ENABLE_LAZY_BASIN.get();
        enable_lazy_item_drain = ENABLE_LAZY_ITEM_DRAIN.get();
        enable_cache_crafter = ENABLE_CACHE_CRAFTER.get();
        enable_cache_crafter_debugger = ENABLE_CACHE_CRAFTER_DEBUGGER.get();
        enable_lazy_crafter_redstone = ENABLE_LAZY_CRAFTER_REDSTONE.get();
        enable_lazy_arm = ENABLE_LAZY_ARM.get();
        enable_cache_deployer = ENABLE_CACHE_DEPLOYER.get();

        arm_ignore_lazytick_list = ARM_IGNORE_LAZYTICK_LIST.get();
        arm_weak_lazytick_list = ARM_WEAK_LAZYTICK_LIST.get();

        funnel_delay_max = FUNNEL_DELAY_MAX.get();
        chute_delay_max = CHUTE_DELAY_MAX.get();
        depot_delay_max = DEPOT_DELAY_MAX.get();
        saw_cache_max = SAW_CACHE_MAX.get();
        belt_delay_max = BELT_DELAY_MAX.get();
        item_drain_delay_max = ITEM_DRAIN_DELAY_MAX.get();
        crafter_global_cache_max = CRAFTER_GLOBAL_CACHE_MAX.get();
        crafter_cache_record_delay = CRAFTER_CACHE_RECORD_DELAY.get();
        crafter_redstone_delay_max = CRAFTER_REDSTONE_DELAY_MAX.get();
        arm_weak_delay_max = ARM_WEAK_DELAY_MAX.get();
        arm_delay_max = ARM_DELAY_MAX.get();
    }
}