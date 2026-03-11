package net.pinkcats.createlazytick.Register;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.config.ServerConfig;

public class ClientInit {

    public static void initClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ModContainer container, Screen screen) -> new BaseConfigScreen(screen, CreateLazyTick.MODID)
                        .withSpecs(ClientConfig.SPEC, null, ServerConfig.SPEC)
        );
    }
}
