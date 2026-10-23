package net.pinkcats.createlazytick.Register;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.pinkcats.NutUI.menu.ClientScreen;
import net.pinkcats.NutUI.menu.Connect.ClientNutNetwork;
import net.pinkcats.createlazytick.Gui.Menu.MenuClientInit;
import net.pinkcats.createlazytick.client.LazyTickClockHintOverlay;
import net.pinkcats.createlazytick.Channel.ClientClockNetwork;

@Environment(EnvType.CLIENT)
public final class ClientInit {
    private ClientInit() {}

    public static void initClient() {
        ClientClockNetwork.registerReceivers();
        ClientNutNetwork.registerReceivers();
        MenuClientInit.registerScreens();
        ClientScreen.registerScreen();
        LazyTickClockHintOverlay.register();
    }
}
