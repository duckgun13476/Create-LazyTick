package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.core.BlockPos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SharedData {
    private static int Dimension = 0;
    public static  boolean state_apply = false;
    public static int state_local = 2;
    private static List<CoordinateData> coordinatesList = new ArrayList<>();
    private static List<CoordinateData> other_coordinatesList = new ArrayList<>();




    public static boolean getState_apply() {return state_apply;}
    public static void setState_apply(boolean state_apply) {
        SharedData.state_apply = state_apply;
    }
    public static int getState_local() {return state_local;}
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


    // 根据 ID 查找 CoordinateData 对象
    public static CoordinateData findCoordinateDataById(String id) {
        if (coordinatesList == null) {
            coordinatesList = new ArrayList<>(); // 确保它被初始化
        }
        for (CoordinateData coordinateData : coordinatesList) {
            if (coordinateData.getId().equals(id)) {
                return coordinateData;
            }
        }
        return null; // 如果未找到，返回 null
    }

    // 根据坐标数组查找 CoordinateData 对象
    public static CoordinateData findCoordinateDataByCoordinates(int[] coordinates) {
        if (coordinatesList == null) {
            coordinatesList = new ArrayList<>(); // 确保它被初始化
        }

        // 确保输入的数组长度为 3
        if (coordinates.length != 3) {
            throw new IllegalArgumentException("Coordinates array must have exactly 3 elements.");
        }

        for (CoordinateData coordinateData : coordinatesList) {
            // 假设 getCoordinates() 返回一个 int[] 数组
            int[] dataCoordinates = coordinateData.getPos();

            // 比较两个数组
            if (dataCoordinates.length == 3 &&
                    dataCoordinates[0] == coordinates[0] &&
                    dataCoordinates[1] == coordinates[1] &&
                    dataCoordinates[2] == coordinates[2]) {
                return coordinateData;
            }
        }
        return null; // 如果未找到，返回 null
    }

    // 将整个列表转换为字符串
    public static String coordinatesListToString(List<CoordinateData> list) {
        StringBuilder sb = new StringBuilder();
        for (CoordinateData coordinateData : list) {
            sb.append(coordinateData.toNBT().toString()).append("\n"); // 用换行符分隔
        }
        return sb.toString().trim(); // 去掉最后的换行符
    }

    public static void setCloneCoordinatesList(List<CoordinateData> load_list) {
        coordinatesList.clear(); // 清空原有列表
        coordinatesList.addAll(load_list); // 添加新列表的内容
    }


    public static byte[] serializeCoordinateDataList(List<CoordinateData> coordinateDataList)  {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(coordinateDataList);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CoordinateData> deserializeCoordinateDataList(byte[] data) throws IOException, ClassNotFoundException {
        try (

                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)
        ) {

            return (List<CoordinateData>) ois.readObject();
        }
        catch (ClassNotFoundException e) {

        }
        return List.of();
    }


}
