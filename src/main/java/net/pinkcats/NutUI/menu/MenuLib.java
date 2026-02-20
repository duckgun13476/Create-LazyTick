package net.pinkcats.NutUI.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.pinkcats.NutUI.menu.architect.NutItemMenu;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class MenuLib {


    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES,MODID);


    public static final RegistryObject<MenuType<NutItemMenu>> ItemMenuRegiste =
            MENU_TYPES.register("transfer_menu",()->
                    IForgeMenuType.create((windowId, inventory, friendlyByteBuf) ->
                            new NutItemMenu( inventory,windowId, friendlyByteBuf.readBlockPos())));



    public static void init(IEventBus IEventBus) {
        MENU_TYPES.register(IEventBus);
    }
}
