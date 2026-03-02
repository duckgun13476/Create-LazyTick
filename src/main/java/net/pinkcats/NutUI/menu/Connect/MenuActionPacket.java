package net.pinkcats.NutUI.menu.Connect;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.pinkcats.NutUI.menu.NutKineticMenu;
import net.pinkcats.NutUI.menu.extensions.NutMenuExtensionRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MenuActionPacket implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    private final String action;
    private final Map<String, String> variables;

    public static final Type<MenuActionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("createlazytick", "menu_action"));

    public static final StreamCodec<FriendlyByteBuf, MenuActionPacket> STREAM_CODEC = StreamCodec.ofMember(
            MenuActionPacket::encode,
            MenuActionPacket::new
    );

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

    public void handle(IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sender)) {
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
        }
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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
