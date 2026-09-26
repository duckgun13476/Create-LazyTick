package net.pinkcats.NutUI.menu.Connect;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.extensions.NutMenuExtensionRegistry;

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

            Map<String, Object> decodedVariables = decodeVariables(variables);
            NutKineticMenu.NutItemMenu menu = resolveTargetMenu(sender, decodedVariables);
            if (menu == null) {
                return;
            }

            menu.handleClientAction(sender, action, decodedVariables);
            Channel.syncMenuDataToPlayer(sender, Channel.currentDimensionId(sender), menu.buildAutoSyncVariables());
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static NutKineticMenu.NutItemMenu resolveTargetMenu(ServerPlayer sender, Map<String, Object> variables) {
        if (sender.containerMenu instanceof NutKineticMenu.NutItemMenu openMenu) {
            return openMenu;
        }

        ResourceLocation menuId = readMenuId(variables.get("menu_id"));
        BlockPos pos = readBlockPos(variables);
        if (menuId == null || pos == null) {
            return null;
        }

        return NutMenuExtensionRegistry.createMenu(sender.getInventory(), sender.containerMenu.containerId, sender, pos, menuId);
    }

    private static ResourceLocation readMenuId(Object value) {
        if (!(value instanceof String menuId) || menuId.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(menuId);
    }

    private static BlockPos readBlockPos(Map<String, Object> variables) {
        Integer x = asInt(variables.get("pos_x"));
        Integer y = asInt(variables.get("pos_y"));
        Integer z = asInt(variables.get("pos_z"));
        if (x == null || y == null || z == null) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
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
