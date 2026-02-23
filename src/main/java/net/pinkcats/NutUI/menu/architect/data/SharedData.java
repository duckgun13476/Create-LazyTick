package net.pinkcats.NutUI.menu.architect.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedData {
    private static final Gson GSON = new Gson();

    private static int Dimension = 0;
    public static boolean state_apply = false;
    public static int state_local = 2;
    private static List<CoordinateData> coordinatesList = new ArrayList<>();
    private static List<CoordinateData> other_coordinatesList = new ArrayList<>();
    private static Map<String, String> syncedVariables = new HashMap<>();

    public static boolean getState_apply() {
        return state_apply;
    }

    public static void setState_apply(boolean state_apply) {
        SharedData.state_apply = state_apply;
    }

    public static int getState_local() {
        return state_local;
    }

    public static void setState_local(int state_local) {
        SharedData.state_local = state_local;
    }

    public static int getDimension() {
        return Dimension;
    }

    public static void setDimension(int value) {
        Dimension = value;
    }

    public static int[] BP_To_Coordinates(BlockPos blockPos) {
        return new int[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()};
    }

    public static void addCoordinateData(String id, int[] pos, int[] values, int state) {
        coordinatesList.add(new CoordinateData(id, pos, values, state));
    }

    public static List<CoordinateData> getCoordinatesList() {
        return coordinatesList;
    }

    public static void setCoordinatesList(List<CoordinateData> load_list) {
        coordinatesList = load_list;
    }

    public static CoordinateData findCoordinateDataById(String id) {
        if (coordinatesList == null) {
            coordinatesList = new ArrayList<>();
        }
        for (CoordinateData coordinateData : coordinatesList) {
            if (coordinateData.getId().equals(id)) {
                return coordinateData;
            }
        }
        return null;
    }

    public static CoordinateData findCoordinateDataByCoordinates(int[] coordinates) {
        if (coordinatesList == null) {
            coordinatesList = new ArrayList<>();
        }

        if (coordinates.length != 3) {
            throw new IllegalArgumentException("Coordinates array must have exactly 3 elements.");
        }

        for (CoordinateData coordinateData : coordinatesList) {
            int[] dataCoordinates = coordinateData.getPos();
            if (dataCoordinates.length == 3 &&
                    dataCoordinates[0] == coordinates[0] &&
                    dataCoordinates[1] == coordinates[1] &&
                    dataCoordinates[2] == coordinates[2]) {
                return coordinateData;
            }
        }
        return null;
    }

    public static String coordinatesListToString(List<CoordinateData> list) {
        StringBuilder sb = new StringBuilder();
        for (CoordinateData coordinateData : list) {
            sb.append(coordinateData.toNBT().toString()).append("\n");
        }
        return sb.toString().trim();
    }

    public static void setCloneCoordinatesList(List<CoordinateData> load_list) {
        coordinatesList.clear();
        coordinatesList.addAll(load_list);
    }

    public static byte[] serializeCoordinateDataList(List<CoordinateData> coordinateDataList) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(coordinateDataList);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CoordinateData> deserializeCoordinateDataList(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (List<CoordinateData>) ois.readObject();
        } catch (ClassNotFoundException e) {
        }
        return List.of();
    }

    public static void setSyncedVariables(Map<String, String> variables) {
        if (variables == null) {
            syncedVariables = new HashMap<>();
            return;
        }
        syncedVariables = new HashMap<>(variables);
    }

    public static Map<String, String> getSyncedVariablesRaw() {
        return new HashMap<>(syncedVariables);
    }

    public static boolean hasSyncedVariable(String key) {
        return syncedVariables.containsKey(key);
    }

    public static JsonElement getSyncedVariable(String key) {
        String raw = syncedVariables.get(key);
        if (raw == null) {
            return null;
        }
        return JsonParser.parseString(raw);
    }

    public static int getSyncedInt(String key, int defaultValue) {
        JsonElement element = getSyncedVariable(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return defaultValue;
        }
        return element.getAsInt();
    }

    public static boolean getSyncedBoolean(String key, boolean defaultValue) {
        JsonElement element = getSyncedVariable(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        return element.getAsBoolean();
    }

    public static String getSyncedString(String key, String defaultValue) {
        JsonElement element = getSyncedVariable(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    public static <T> T getSyncedObject(String key, Class<T> type) {
        JsonElement element = getSyncedVariable(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return GSON.fromJson(element, type);
    }
}
