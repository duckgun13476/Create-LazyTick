package net.pinkcats.createlazytick.Register;

import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

import java.util.function.Supplier;

import static net.minecraft.world.item.Rarity.EPIC;

public class LazyTickItem {
    private static Item clock;
    public static final Supplier<Item> CLOCK = () -> clock;

    public static void register() {
        clock = Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(CreateLazyTick.MODID, "clock"),
                new LazyTickClockItem(new Item.Properties().stacksTo(1).rarity(EPIC)));
    }
}
