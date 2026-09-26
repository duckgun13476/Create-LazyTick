package net.pinkcats.createlazytick.Gui.Menu;

/** Kept as a compatibility entry for former Forge client setup callers. */
public final class ClientModEvents {
    private ClientModEvents() {}

    public static void onClientSetup() {
        MenuClientInit.registerScreens();
    }
}
