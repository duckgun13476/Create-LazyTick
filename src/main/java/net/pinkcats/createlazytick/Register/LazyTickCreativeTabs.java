package net.pinkcats.createlazytick.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.pinkcats.createlazytick.CreateLazyTick;

public class LazyTickCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateLazyTick.MODID);

    public static final RegistryObject<CreativeModeTab> LAZY_TICK_TAB = CREATIVE_MODE_TABS.register("lazy_tick_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(LazyTickItem.CLOCK.get()))
                    .title(Component.translatable("creativetab.createlazytick_tab"))
                    .displayItems((pParameters, pOutput) -> pOutput.accept(LazyTickItem.CLOCK.get()))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
