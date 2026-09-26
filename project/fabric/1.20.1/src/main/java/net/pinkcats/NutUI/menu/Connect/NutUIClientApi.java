package net.pinkcats.NutUI.menu.Connect;

import java.util.Map;

public final class NutUIClientApi {
    private NutUIClientApi() {
    }

    public static void sendAction(String action, Map<String, ?> variables) {
        Channel.sendActionToServer(new MenuActionPacket(action, variables));
    }

    public static void sendAction(String action, String key, Object value) {
        sendAction(action, Map.of(key, value));
    }
}
