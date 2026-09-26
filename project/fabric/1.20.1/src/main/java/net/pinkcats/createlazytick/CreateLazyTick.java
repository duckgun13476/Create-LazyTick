package net.pinkcats.createlazytick;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.createlazytick.Gui.Menu.MenuInit;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.Register.AllChannel;
import net.pinkcats.NutUI.menu.Connect.Channel;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.slf4j.Logger;

import static net.pinkcats.createlazytick.Register.LazyTickCommand.RegisterCLTCommand;
import static net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex.isBasinOptimizationSafe;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.AMOUNT_CACHE;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CAN_FILL_CACHE;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CrafterRecipeCache;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.IsCrafterCacheFull;

public final class CreateLazyTick {
    public static final String MODID = "createlazytick";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static volatile boolean IsServerReload = false;
    private static int cacheReloadTick = 0;

    private CreateLazyTick() {}

    public static ResourceLocation DropResourceLocation(String location) {
        return new ResourceLocation(location);
    }

    public static ResourceLocation DropResourceLocation(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static void initialize() {
        LazyTickItem.register();
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> {
            if (tab == AllCreativeModeTabs.BASE_CREATIVE_TAB.tab()) {
                entries.accept(LazyTickItem.CLOCK.get());
            }
        });
        AllChannel.registerChannel();
        Channel.ensurePacketsRegistered();
        NutKineticMenu.init();
        MenuInit.registerCommon();
        ForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, ServerConfig.SPEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            clearRecipeCaches();
            BasinRecipeIndex.rebuild(server.getRecipeManager());
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                clearRecipeCaches();
                isBasinOptimizationSafe = true;
                BasinRecipeIndex.rebuild(server.getRecipeManager());
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RegisterCLTCommand(dispatcher));
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (IsServerReload && ++cacheReloadTick > ServerConfig.getGlobalCacheRecordDelay()) {
                cacheReloadTick = 0;
                IsServerReload = false;
            }
        });

        if (isClient()) {
            ClientBootstrap.init();
        }
    }

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
        private ClientBootstrap() {}

        static void init() {
            try {
                Class<?> clientInit = Class.forName("net.pinkcats.createlazytick.Register.ClientInit");
                clientInit.getMethod("initClient").invoke(null);
            } catch (ReflectiveOperationException error) {
                throw new RuntimeException("CreateLazyTick failed to initialize client bootstrap", error);
            }
        }
    }
}
