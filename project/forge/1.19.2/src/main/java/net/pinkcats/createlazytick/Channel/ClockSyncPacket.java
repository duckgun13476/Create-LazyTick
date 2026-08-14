package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClockSyncPacket {

    private final BlockPos pos;
    private final String dimension;
    private final int extraData;
    private final boolean isQuery;

    private static final int MAX_PENDING_REQUESTS = 80;
    private static final long PENDING_REQUEST_TTL_MILLIS = 10_000L;
    private static final Map<RequestTarget, LinkedHashMap<Integer, ClientData>> PENDING_REQUESTS = new LinkedHashMap<>();
    private static int pendingRequestCount = 0;

    // 构造函数 1: 纯查询 (Tooltip 用)
    public ClockSyncPacket(BlockPos pos) {
        this.pos = pos;
        this.dimension = "";
        this.extraData = 0;
        this.isQuery = true; // 标记为查询
    }

    // [保留] 构造函数 2: 旧逻辑兼容 (设置用)
    public ClockSyncPacket(int extraData , String dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
        this.extraData = extraData;
        this.isQuery = false; // 标记为设置
    }

    public ClockSyncPacket(FriendlyByteBuf buf) {
        dimension = buf.readUtf();
        pos = buf.readBlockPos();
        extraData = buf.readInt();
        isQuery = buf.readBoolean(); // [新增] 读取标记
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeInt(extraData);
        buf.writeBoolean(isQuery);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.setPacketHandled(true);

        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        // Move to Main loop
        ctx.enqueueWork(() -> {
            // 查询模式
            if (isQuery) {
                Level level = player.level;
                if (level.isLoaded(pos)) {
                    if (level.getBlockEntity(pos) instanceof ISmartBlockEntityControl control) {
                        control.createLazyTick$sendBlockUpdated();
                    }
                }
                return;
            }

            enqueuePendingRequest(new ClientData(extraData, dimension, pos));
        });
    }

    public static ClientData takePendingRequest(String dimension, BlockPos pos) {
        RequestTarget target = new RequestTarget(dimension, pos.immutable());
        LinkedHashMap<Integer, ClientData> requests = PENDING_REQUESTS.get(target);
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        Iterator<ClientData> iterator = requests.values().iterator();
        ClientData request = iterator.next();
        iterator.remove();
        pendingRequestCount--;
        if (requests.isEmpty()) {
            PENDING_REQUESTS.remove(target);
        }
        return request;
    }

    private static void enqueuePendingRequest(ClientData data) {
        purgeExpiredRequests(System.currentTimeMillis());

        RequestTarget target = new RequestTarget(data.getDimension(), data.getPos().immutable());
        LinkedHashMap<Integer, ClientData> requests = PENDING_REQUESTS.computeIfAbsent(target, ignored -> new LinkedHashMap<>());
        if (requests.containsKey(data.getExtraData())) {
            return;
        }
        if (pendingRequestCount >= MAX_PENDING_REQUESTS) {
            mes.error("ServerPacket Cargo is full. This shouldn't happen!");
            return;
        }
        requests.put(data.getExtraData(), data);
        pendingRequestCount++;
    }

    private static void purgeExpiredRequests(long now) {
        Iterator<Map.Entry<RequestTarget, LinkedHashMap<Integer, ClientData>>> targetIterator = PENDING_REQUESTS.entrySet().iterator();
        while (targetIterator.hasNext()) {
            LinkedHashMap<Integer, ClientData> requests = targetIterator.next().getValue();
            Iterator<ClientData> requestIterator = requests.values().iterator();
            while (requestIterator.hasNext()) {
                if (now - requestIterator.next().getCreatedAtMillis() > PENDING_REQUEST_TTL_MILLIS) {
                    requestIterator.remove();
                    pendingRequestCount--;
                }
            }
            if (requests.isEmpty()) {
                targetIterator.remove();
            }
        }
    }

    private record RequestTarget(String dimension, BlockPos pos) {}


    @Override
    public String toString() {
        return "Packet{" +
                "dimension=" + dimension +
                ", pos="  + pos +
                ", extraData="  + extraData +
                '}';
    }
}
