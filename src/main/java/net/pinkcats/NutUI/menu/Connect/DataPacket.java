package net.pinkcats.NutUI.menu.Connect;



import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.NutUI.menu.architect.data.CoordinateData;
import net.pinkcats.NutUI.menu.architect.data.SharedData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DataPacket {

    private final int dimension; // 使用小写字母开头的变量名
    private final List<CoordinateData> entityList;

    public DataPacket(int dimension, List<CoordinateData> entityList) {
        this.dimension = dimension;
        this.entityList = entityList;
    }

    public DataPacket(FriendlyByteBuf buf) {
        dimension = buf.readInt();
        int size = buf.readInt();
        this.entityList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CoordinateData data = CoordinateData.decode(buf); // 通过 buf 创建 CoordinateData 实例
            entityList.add(data);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dimension); // 编码维度
        buf.writeInt(entityList.size()); // 编码列表大小
        for (CoordinateData entity : entityList) {
            entity.encode(buf); // 编码每个 CoordinateData
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        SharedData.setDimension(dimension); // 设置维度
        SharedData.setCoordinatesList(entityList); // 设置实体列表
        ctx.setPacketHandled(true); // 标记数据包已处理
        return true;
    }

    public int getDimension() {
        return dimension;
    }
    public List<CoordinateData> getEntityList() {
        return entityList;
    }

    @Override
    public String toString() {
        return "DataPacket{" +
                "dimension=" + dimension +
                ", entityList=" + entityList +
                '}';
    }


}
