package net.pinkcats.NutUI.menu.Connect;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.NutUI.menu.NutKineticMenu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MenuActionPacket {
    private static final Gson GSON = new Gson();

    private final String action;
    private final Map<String, String> variables;

    public MenuActionPacket(String action, Map<String, ?> variables) {
        this.action = action == null ? "" : action;
        this.variables = encodeVariables(variables);
    }

    public MenuActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf(32767);
        int size = buf.readInt();
        Map<String, String> readVariables = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            readVariables.put(buf.readUtf(32767), buf.readUtf(32767));
        }
        this.variables = readVariables;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeInt(variables.size());
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }
            if (!(sender.containerMenu instanceof NutKineticMenu.NutItemMenu menu)) {
                return;
            }

            menu.handleClientAction(sender, action, decodeVariables(variables));
            Channel.syncMenuDataToPlayer(sender, menu.count, menu.buildAutoSyncVariables());
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static Map<String, String> encodeVariables(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>(source.size());
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            out.put(entry.getKey(), value == null ? "null" : GSON.toJson(value));
        }
        return out;
    }

    private static Map<String, Object> decodeVariables(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> out = new HashMap<>(source.size());
        for (Map.Entry<String, String> entry : source.entrySet()) {
            JsonElement element = JsonParser.parseString(entry.getValue());
            out.put(entry.getKey(), GSON.fromJson(element, Object.class));
        }
        return out;
    }
}
