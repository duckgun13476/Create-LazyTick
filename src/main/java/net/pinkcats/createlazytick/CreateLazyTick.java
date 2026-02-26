package net.pinkcats.createlazytick;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.config.ClientConfig;
import org.slf4j.Logger;

import java.lang.reflect.Field;

import static net.pinkcats.createlazytick.Register.LazyTickCommand.RegisterCLTCommand;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateLazyTick.MODID)
public class CreateLazyTick {
    public static final String MODID = "createlazytick";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static volatile boolean IsServerReload = false;

    public static ResourceLocation DropResourceLocation(String Location){
        return ResourceLocation.parse(Location);
    }
    public static ResourceLocation DropResourceLocation(String NameSpace, String Path){
        return ResourceLocation.fromNamespaceAndPath(NameSpace,Path);
    }

    /** If you can't use level.isClientSide(),use this.</p>
     * Especially for LazyTickScrollBehaviour.addTo()
     * **/
    public static boolean isClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT;
    }


    public CreateLazyTick() {

        // Only for 1.20.1 forge
        ModLoadingContext modLoadingContext = getModLoadingContextViaReflection();
        FMLJavaModLoadingContext modContext = modLoadingContext.extension();
        IEventBus modEventBus = modContext.getModEventBus();

        LazyTickItem.register(modEventBus);

        // From UI Lib
        NutKineticMenu.init(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        DistExecutor.safeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> net.pinkcats.createlazytick.Register.ClientInit::initClient
        );


    }


    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        BasinRecipeIndex.rebuild(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return; // 忽略单个玩家的数据包同步，仅在全局重载之后触发全部配方索引重构
        BasinRecipeIndex.rebuild(event.getPlayerList().getServer().getRecipeManager());
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        RegisterCLTCommand(event);
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }


        @SubscribeEvent
        public static void addToCreateTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTab() == AllCreativeModeTabs.BASE_CREATIVE_TAB.get()) {
                event.accept(LazyTickItem.CLOCK.get());
            }
        }
    }




    //Tool Func


    @SuppressWarnings("unchecked")
    public static ModLoadingContext getModLoadingContextViaReflection() {
        try {
            Field contextField = ModLoadingContext.class.getDeclaredField("context");
            contextField.setAccessible(true);
            ThreadLocal<ModLoadingContext> contextThreadLocal = (ThreadLocal<ModLoadingContext>) contextField.get(null);
            return contextThreadLocal.get();

        } catch (Exception e) {
            throw new RuntimeException("CreateLazyTick got ERROR in Init:", e);
        }
    }
}
