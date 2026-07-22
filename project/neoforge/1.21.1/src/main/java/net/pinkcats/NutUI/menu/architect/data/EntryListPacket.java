package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntryListPacket implements CustomPacketPayload {

    public static final Type<EntryListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("createlazytick", "entry_list"));

    public static final StreamCodec<FriendlyByteBuf, EntryListPacket> STREAM_CODEC = StreamCodec.ofMember(
            EntryListPacket::encode,
            EntryListPacket::new
    );

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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        // 处理接收到的条目列表，例如存储或打印
        ctx.enqueueWork(() -> {
            EntryList.set(entryList);
            //  System.out.println("Received entry list: " + entryList);
        });
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
