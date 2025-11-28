package net.pinkcats.createlazytick;

import net.minecraftforge.common.ForgeConfigSpec;
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


    // funnel
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_FUNNEL = BUILDER
            .comment("Whether to enable funnel lazy tick")
            .define("enable_lazy_funnel", true);
    private static final ForgeConfigSpec.IntValue FUNNEL_DELAY_MAX = BUILDER
            .comment("max delay tick if funnel is rest")
            .defineInRange("magicNumber", 120, 20, Integer.MAX_VALUE);


    // chute
    private static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_CHUTE = BUILDER
            .comment("Whether to enable chute lazy tick")
            .define("enable_lazy_chute", true);
    private static final ForgeConfigSpec.IntValue CHUTE_DELAY_MAX = BUILDER
            .comment("max delay tick if funnel is rest")
            .defineInRange("magicNumber", 60, 20, Integer.MAX_VALUE);


    //=================================================================================================


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enable_lazy_tick;
    public static boolean enable_lazy_funnel;
    public static boolean enable_lazy_chute;


    public static int funnel_delay_max;
    public static int chute_delay_max;



    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enable_lazy_tick = ENABLE_LAZY_TICK.get();
        enable_lazy_funnel = ENABLE_LAZY_FUNNEL.get();
        enable_lazy_chute = ENABLE_LAZY_CHUTE.get();
        funnel_delay_max = FUNNEL_DELAY_MAX.get();
        chute_delay_max = CHUTE_DELAY_MAX.get();


    }
}
