package net.pinkcats.createlazytick.Channel;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static net.pinkcats.createlazytick.CreateLazyTick.DropResourceLocation;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class CLTChannel {

    private static final String PROTOCOL_VERSION = "2";


    public static final SimpleChannel INSTANCE_TO_SERVER = NetworkRegistry.newSimpleChannel(
            DropResourceLocation(MODID,"dimension_to_server"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            s -> true
    );
    public static void register_to_server() {
        INSTANCE_TO_SERVER.messageBuilder(ClockSyncPacket.class,1, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClockSyncPacket::new)
                .encoder(ClockSyncPacket::encode)
                .consumerMainThread(ClockSyncPacket::handle)
                .add();
        INSTANCE_TO_SERVER.messageBuilder(LazyTickStatePacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(LazyTickStatePacket::new)
                .encoder(LazyTickStatePacket::encode)
                .consumerMainThread(LazyTickStatePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE_TO_SERVER.sendToServer(message);
    }

    public static void sendToPlayer(LazyTickStatePacket message, ServerPlayer player) {
        INSTANCE_TO_SERVER.send(PacketDistributor.PLAYER.with(() -> player), message);
    }


}
