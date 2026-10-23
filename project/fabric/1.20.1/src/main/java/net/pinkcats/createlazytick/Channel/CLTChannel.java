package net.pinkcats.createlazytick.Channel;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public final class CLTChannel {
    public static final ResourceLocation CLOCK_SYNC = new ResourceLocation(MODID, "dimension_to_server");
    public static final ResourceLocation CLOCK_STATE = new ResourceLocation(MODID, "lazytick_state");

    private CLTChannel() {}

    public static void register_to_server() {
        ServerPlayNetworking.registerGlobalReceiver(CLOCK_SYNC, (server, player, handler, buf, responseSender) -> {
            ClockSyncPacket packet = new ClockSyncPacket(buf);
            server.execute(() -> packet.handle(player));
        });
    }

    public static void sendToServer(ClockSyncPacket packet) {
        ClientClockNetwork.send(packet);
    }

    public static void sendToPlayer(LazyTickStatePacket packet, ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, CLOCK_STATE)) return;
        var buffer = PacketByteBufs.create();
        packet.encode(buffer);
        ServerPlayNetworking.send(player, CLOCK_STATE, buffer);
    }
}
