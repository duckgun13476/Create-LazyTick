package net.pinkcats.createlazytick;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CreateLazyTick.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{


    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_TICK = BUILDER
            .comment("Whether to enable lazy tick mixin")
            .define("enable_lazy_tick", true);
    //=================================================================================================

    // fluid_pipe
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FLUID = BUILDER
            .comment("Whether to enable fluid pipe lazy tick")
            .define("enable_lazy_fluid", true);

    // funnel
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FUNNEL = BUILDER
            .comment("Whether to enable funnel lazy tick")
            .define("enable_lazy_funnel", true);
    private static final ForgeConfigSpec.IntValue FUNNEL_DELAY_MAX = BUILDER
            .comment("max delay tick if funnel is rest")
            .defineInRange("funnel_delay_max", 120, 20, Integer.MAX_VALUE);


    // chute
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CHUTE = BUILDER
            .comment("Whether to enable chute lazy tick")
            .define("enable_lazy_chute", true);
    private static final ForgeConfigSpec.IntValue CHUTE_DELAY_MAX = BUILDER
            .comment("max delay tick if chute is rest")
            .defineInRange("chute_delay_max", 60, 20, Integer.MAX_VALUE);

    // depot
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_DEPOT = BUILDER
            .comment("Whether to enable depot lazy tick")
            .define("enable_lazy_depot", true);
    private static final ForgeConfigSpec.IntValue DEPOT_DELAY_MAX = BUILDER
            .comment("max delay tick if depot is rest")
            .defineInRange("depot_delay_max", 60, 20, Integer.MAX_VALUE);


    // belt
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BELT = BUILDER
            .comment("Whether to enable belt lazy tick")
            .define("enable_lazy_belt", true);
    private static final ForgeConfigSpec.IntValue BELT_DELAY_MAX = BUILDER
            .comment("max delay tick if belt is rest")
            .defineInRange("belt_delay_max", 60, 20, Integer.MAX_VALUE);



    // saw
    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE_SAW = BUILDER
            .comment("Whether to enable saw cache to improve efficiency of finding recipes")
            .define("enable_lazy_saw", true);
    private static final ForgeConfigSpec.IntValue SAW_CACHE_MAX = BUILDER
            .comment("max cache count")
            .defineInRange("saw_delay_max", 50, 1, Integer.MAX_VALUE);


    // basin -> mixer/press
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_BASIN = BUILDER
            .comment("Whether to enable basin lazy tick(Influence on mechanical mixer and mechanical press)")
            .define("enable_lazy_basin",true);
    private static final ForgeConfigSpec.IntValue BASIN_DELAY_MAX = BUILDER
            .comment("max delay tick if basin is rest")
            .defineInRange("basin_delay_max", 60, 20, Integer.MAX_VALUE);

    //item_drain(transfer item)
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ITEM_DRAIN = BUILDER
            .comment("Whether to enable item drain lazy tick")
            .define("enable_lazy_item_drain", true);
    private static final ForgeConfigSpec.IntValue ITEM_DRAIN_DELAY_MAX = BUILDER
            .comment("max delay tick if something stuck on the item drain")
            .defineInRange("item_drain_delay_max" ,60 ,20, Integer.MAX_VALUE);
    //=================================================================================================


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enable_lazy_tick;

    public static boolean enable_lazy_fluid;
    public static boolean enable_lazy_funnel;
    public static boolean enable_lazy_chute;
    public static boolean enable_lazy_depot;
    public static boolean enable_cache_saw;
    public static boolean enable_belt_delay;
    public static boolean enable_lazy_basin;
    public static boolean enable_lazy_item_drain;

    public static int funnel_delay_max;
    public static int chute_delay_max;
    public static int depot_delay_max;
    public static int saw_cache_max;
    public static int belt_delay_max;
    public static int basin_delay_max;
    public static int item_drain_delay_max;

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

        funnel_delay_max = FUNNEL_DELAY_MAX.get();
        chute_delay_max = CHUTE_DELAY_MAX.get();
        depot_delay_max = DEPOT_DELAY_MAX.get();
        saw_cache_max = SAW_CACHE_MAX.get();
        belt_delay_max = BELT_DELAY_MAX.get();
        basin_delay_max = BASIN_DELAY_MAX.get();
        item_drain_delay_max = ITEM_DRAIN_DELAY_MAX.get();
    }
}
