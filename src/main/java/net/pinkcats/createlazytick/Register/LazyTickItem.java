package net.pinkcats.createlazytick.Register;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

import static net.minecraft.world.item.Rarity.EPIC;

public class LazyTickItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, CreateLazyTick.MODID);

    public static final RegistryObject<Item> CLOCK = ITEMS.register("clock",
            () -> new LazyTickClockItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
