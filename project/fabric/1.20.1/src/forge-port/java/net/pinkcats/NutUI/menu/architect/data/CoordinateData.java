package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.io.Serializable;

public class CoordinateData implements Serializable {
    private final String id;           // ID 字符串
    private final int[] entity_pos;    // xyz 坐标数组
    private final int[] Dimensions;     // 三个 int 数字构成的数组
    private int state;                 // 界面状态

    // 克隆方法
    public CoordinateData clone() {
        int[] clonedEntityPos = entity_pos.clone(); // 深拷贝坐标数组
        int[] clonedDimensions = Dimensions.clone(); // 深拷贝 Dimensions 数组
        return new CoordinateData(id, clonedEntityPos, clonedDimensions, state);
    }

    public CoordinateData(String id, int[] entity_pos, int[] Dimensions, int state) {
        this.id = id;
        this.entity_pos = entity_pos;
        this.Dimensions = Dimensions;
        this.state = state;
    }

    public CoordinateData(String id, int[] entityPos, int[] dimensions, int state, int dx, int dy, int dz, String id1, int[] entityPos1, int[] dimensions1) {
        this.id = id1;
        entity_pos = entityPos1;
        Dimensions = dimensions1;
    }

    public void setDimensionsX(int x) {
        this.Dimensions[0] = x;
    }

    public void setDimensionsY(int y) {
        this.Dimensions[1] = y;
    }

    public void setDimensionsZ(int z) {
        this.Dimensions[2] = z;
    }

    public void SetState(int state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public int[] getPos() {
        return entity_pos;
    }

    public int[] getDimensions() {
        return Dimensions;
    }

    public int getState() {
        return state;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putIntArray("entity_pos", entity_pos);
        tag.putIntArray("Dimensions", Dimensions);
        tag.putInt("state", state);
        return tag;
    }

    public static CoordinateData fromNBT(CompoundTag tag) {
        String id = tag.getString("id");
        int[] pos = tag.getIntArray("entity_pos");
        int[] Dimensions = tag.getIntArray("Dimensions");
        int state = tag.getInt("state");
        return new CoordinateData(id, pos, Dimensions, state);
    }

    // 自定义方法来编码 int 数组
    public static void writeIntArray(FriendlyByteBuf buf, int[] array) {
        buf.writeInt(array.length); // 写入数组长度
        for (int value : array) {
            buf.writeInt(value); // 写入每个 int 值
        }
    }

    // 自定义方法来解码 int 数组
    public static int[] readIntArray(FriendlyByteBuf buf) {
        int length = buf.readInt(); // 读取数组长度
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = buf.readInt(); // 读取每个 int 值
        }
        return array;
    }



    // 新增编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id); // 编码 ID 字符串
        CoordinateData.writeIntArray(buf,entity_pos); // 编码坐标数组
        CoordinateData.writeIntArray(buf,Dimensions); // 编码 Dimensions 数组
        buf.writeInt(state); // 编码状态
    }

    public CoordinateData(FriendlyByteBuf buf) {
        this.id = buf.readUtf(); // 假设 id 是一个字符串
        this.entity_pos = readIntArray(buf); // 从 buf 读取 int 数组
        this.Dimensions = readIntArray(buf); // 从 buf 读取 int 数组
        this.state = buf.readInt(); // 读取状态
    }
    // 新增解码方法
    public static CoordinateData decode(FriendlyByteBuf buf) {
        String id = buf.readUtf(); // 解码 ID 字符串
        int[] entity_pos = CoordinateData.readIntArray(buf); // 解码坐标数组
        int[] Dimensions = CoordinateData.readIntArray(buf); // 解码 Dimensions 数组
        int state = buf.readInt(); // 解码状态
        return new CoordinateData(id, entity_pos, Dimensions, state);
    }

    @Override
    public String toString() {
        return "CoordinateData{id= " +id+
                "| x=" + entity_pos[0] +
                ", y=" + entity_pos[1] +
                ", z=" + entity_pos[2] +
                "} | dx="+Dimensions[0] + ", dy=" +Dimensions[1]  +", dz=" + Dimensions[2]+ " | "+state;
    }


    public long getTimeStamp() {
        String[] parts = id.split("-");
        return Long.parseLong(parts[1]);
    }
}
