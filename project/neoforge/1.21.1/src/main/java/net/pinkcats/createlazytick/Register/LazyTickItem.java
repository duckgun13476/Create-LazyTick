package net.pinkcats.createlazytick.Register;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

import static net.minecraft.world.item.Rarity.EPIC;

public class LazyTickItem {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateLazyTick.MODID);

    public static final DeferredItem<Item> CLOCK = ITEMS.register("clock",
            () -> new LazyTickClockItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
