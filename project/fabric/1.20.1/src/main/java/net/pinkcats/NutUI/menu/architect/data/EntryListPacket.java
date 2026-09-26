package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class EntryListPacket {

    private final List<Entry> entryList;

    public EntryListPacket(List<Entry> entryList) {
        this.entryList = entryList;
    }

    public EntryListPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.entryList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(32767); // 读取 ID
            String msg = buf.readUtf(32767); // 读取消息
            entryList.add(new Entry(id, msg)); // 创建 Entry 对象并添加到列表
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entryList.size()); // 编码列表大小
        for (Entry entry : entryList) {
            buf.writeUtf(entry.getId()); // 编码每个条目的 ID
            buf.writeUtf(entry.getMsg()); // 编码每个条目的消息
        }
    }

    public void handleClient() {
        EntryList.set(entryList);
    }

    public List<Entry> getEntryList() {
        return entryList;
    }

    @Override
    public String toString() {
        return "EntryListPacket{" +
                "entryList=" + entryList +
                '}';
    }
}
