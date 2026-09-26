package net.pinkcats.createlazytick;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
