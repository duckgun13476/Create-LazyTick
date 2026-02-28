package net.pinkcats.NutUI.menu.Connect;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.NutUI.menu.architect.data.CoordinateData;
import net.pinkcats.NutUI.menu.architect.data.SharedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;

public class DataPacket {

    private static final Gson GSON = new Gson();
    private static final boolean SYNC_DEBUG_LOG = false;
    public static final String DEMO_SYNC_KEY = "nutui_sync_demo";
    public static final String DEMO_SYNC_VERSION_KEY = "nutui_sync_version";
    public static final String DEMO_SYNC_TICK_KEY = "nutui_sync_server_tick";

    private final int dimension;
    private final List<CoordinateData> entityList;
    /**
     * key -> json value string
     */
    private final Map<String, String> variables;

    public DataPacket(int dimension, List<CoordinateData> entityList) {
        this(dimension, entityList, Collections.emptyMap());
    }

    public DataPacket(int dimension, List<CoordinateData> entityList, Map<String, ?> variables) {
        this.dimension = dimension;
        this.entityList = entityList == null ? List.of() : entityList;
        this.variables = encodeVariables(variables);
    }

    public DataPacket(FriendlyByteBuf buf) {
        this.dimension = buf.readInt();

        int size = buf.readInt();
        this.entityList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.entityList.add(CoordinateData.decode(buf));
        }

        int variableSize = buf.readInt();
        Map<String, String> readVariables = new HashMap<>(variableSize);
        for (int i = 0; i < variableSize; i++) {
            readVariables.put(buf.readUtf(32767), buf.readUtf(32767));
        }
        this.variables = readVariables;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(entityList.size());
        for (CoordinateData entity : entityList) {
            entity.encode(buf);
        }

        buf.writeInt(variables.size());
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        if (ctx.getDirection() != null && ctx.getDirection().getReceptionSide().isClient()) {
            SharedData.setDimension(dimension);
            SharedData.setCoordinatesList(entityList);
            SharedData.setSyncedVariables(variables);
        }
        if (SYNC_DEBUG_LOG) {
            String side = ctx.getDirection() == null ? "unknown" : String.valueOf(ctx.getDirection().getReceptionSide());
            LOGGER.info("[NutUI Sync][RECV][{}] dimension={} entityCount={} keys={} demo={} version={} tick={} payload={}",
                    side,
                    dimension,
                    entityList.size(),
                    variables.keySet(),
                    variables.get(DEMO_SYNC_KEY),
                    variables.get(DEMO_SYNC_VERSION_KEY),
                    variables.get(DEMO_SYNC_TICK_KEY),
                    variables);
        }
        ctx.setPacketHandled(true);
        return true;
    }

    public int getDimension() {
        return dimension;
    }

    public List<CoordinateData> getEntityList() {
        return entityList;
    }

    public Map<String, String> getVariablesRaw() {
        return Collections.unmodifiableMap(variables);
    }

    public String getVariableJson(String key) {
        return variables.get(key);
    }

    public JsonElement getVariable(String key) {
        String raw = variables.get(key);
        if (raw == null) {
            return null;
        }
        return JsonParser.parseString(raw);
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

    @Override
    public String toString() {
        return "DataPacket{" +
                "dimension=" + dimension +
                ", entityList=" + entityList +
                ", variables=" + variables +
                '}';
    }
}
