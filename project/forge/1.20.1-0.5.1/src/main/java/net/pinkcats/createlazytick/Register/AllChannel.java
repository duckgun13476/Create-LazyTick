package net.pinkcats.createlazytick.Register;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.pinkcats.createlazytick.Channel.CLTChannel;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD,modid = MODID)
public class AllChannel {
    @SubscribeEvent
    public static void registerChannel(FMLCommonSetupEvent event) {
        CLTChannel.register_to_server();
    }
}
