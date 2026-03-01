package net.pinkcats.createlazytick;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.createlazytick.Gui.Menu.MenuInit;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.config.ClientConfig;

import org.slf4j.Logger;

import java.lang.reflect.Field;

import static net.pinkcats.createlazytick.Register.LazyTickCommand.RegisterCLTCommand;
import static net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex.isBasinOptimizationSafe;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.AMOUNT_CACHE;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CAN_FILL_CACHE;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CrafterRecipeCache;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.IsCrafterCacheFull;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateLazyTick.MODID)
public class CreateLazyTick {
    public static final String MODID = "createlazytick";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static volatile boolean IsServerReload = false;
    private static int cacheReloadTick = 0;

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
        return FMLEnvironment.dist == Dist.CLIENT;
    }


    /*public CreateLazyTick(IEventBus modEventBus, ModContainer modContainer) {


        LazyTickItem.register(modEventBus);

        // From UI Lib
        NutKineticMenu.init(modEventBus);
        MenuInit.registerCommon();

        modEventBus.addListener(this::commonSetup);
        // [新增] 将网络注册监听器直接写在主类中
        modEventBus.addListener(this::registerNetwork);

        // [修改] MinecraftForge.EVENT_BUS 变更为 NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(this);

        // [修改] 直接通过 modContainer 注册 Config
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        if (isClient()) {
            ClientBootstrap.init();
        }
    }*/

    public CreateLazyTick() {

        // Only for 1.21.1 NeoForge
        ModLoadingContext modLoadingContext = getModLoadingContextViaReflection();
        FMLJavaModLoadingContext modContext = modLoadingContext.extension();
        IEventBus modEventBus = modContext.getModEventBus();

        LazyTickItem.register(modEventBus);

        // From UI Lib
        NutKineticMenu.init(modEventBus);
        MenuInit.registerCommon();


        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        if (isClient()) {
            ClientBootstrap.init();
        }


    }


    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        clearRecipeCaches();
        BasinRecipeIndex.rebuild(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return; // 忽略单个玩家的数据包同步，仅在全局重载之后触发全部配方索引重构
        clearRecipeCaches();
        isBasinOptimizationSafe = true;
        BasinRecipeIndex.rebuild(event.getPlayerList().getServer().getRecipeManager());
    }


    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        RegisterCLTCommand(event);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !IsServerReload) {
            return;
        }
        cacheReloadTick++;
        if (cacheReloadTick > ServerConfig.getGlobalCacheRecordDelay()) {
            cacheReloadTick = 0;
            IsServerReload = false;
        }
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

    private static void clearRecipeCaches() {
        IsServerReload = true;
        cacheReloadTick = 0;
        LOGGER.info("[CreateLazyTick] clearing cache...");
        CAN_FILL_CACHE.clear();
        AMOUNT_CACHE.clear();
        CrafterRecipeCache.clear();
        IsCrafterCacheFull = false;
        net.pinkcats.createlazytick.bridge.Crafter.CrafterCacheStats.reset();
        net.pinkcats.createlazytick.bridge.Crafter.CrafterCacheStats.onCooldownSkip();
    }

    private static final class ClientBootstrap {
        private ClientBootstrap() {
        }

        static void init() {
            try {
                Class<?> clientInit = Class.forName("net.pinkcats.createlazytick.Register.ClientInit");
                clientInit.getMethod("initClient").invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("CreateLazyTick failed to initialize client bootstrap", e);
            }
        }
    }


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
