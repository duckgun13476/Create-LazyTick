package net.pinkcats.createlazytick;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.pinkcats.createlazytick.item.LazyTickClockItem;
import org.slf4j.Logger;

import java.lang.reflect.Field;

import static net.pinkcats.createlazytick.command.LazyTickCommand.RegisterCLTCommand;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateLazyTick.MODID)
public class CreateLazyTick {
    public static final String MODID = "createlazytick";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static boolean IsServerReload = false;

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


    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,MODID);

    public static final RegistryObject<Item> CLOCK = ITEMS.register("clock",
            () -> new LazyTickClockItem(new Item.Properties().stacksTo(1)));



    public CreateLazyTick() {

        // Only for 1.20.1 forge
        ModLoadingContext modLoadingContext = getModLoadingContextViaReflection();
        FMLJavaModLoadingContext modContext = modLoadingContext.extension();
        IEventBus modEventBus = modContext.getModEventBus();


        ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

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
    }




    //Tool Func


    @SuppressWarnings("unchecked")
    private static ModLoadingContext getModLoadingContextViaReflection() {
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
