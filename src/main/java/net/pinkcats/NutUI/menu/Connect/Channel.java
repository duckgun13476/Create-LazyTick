package net.pinkcats.NutUI.menu.Connect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pinkcats.NutUI.menu.architect.data.EntryListPacket;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.BuildDefine;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;


public class Channel {

    private static final String PROTOCOL_VERSION = "1";

    private static int id = 0;

    public static final SimpleChannel INSTANCE_TO_SERVER = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID,"dimension_to_server"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    public static void register_to_server() {
        INSTANCE_TO_SERVER.messageBuilder(DataPacket.class,1, NetworkDirection.PLAY_TO_SERVER)
                .decoder(DataPacket::new)
                .encoder(DataPacket::encode)
                .consumerMainThread(DataPacket::handle)
                .add();
    }



    public static final SimpleChannel MSG_TO_CLIENT = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID,"msg_to_client"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    public static void register_msg_to_player() {
        MSG_TO_CLIENT.messageBuilder(EntryListPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(EntryListPacket::new) // 使用 StringListPacket 的构造函数解码
                .encoder(EntryListPacket::encode) // 使用 StringListPacket 的编码方法
                .consumerMainThread(EntryListPacket::handle) // 使用 StringListPacket 的处理方法
                .add();
    }




    public static final SimpleChannel INSTANCE_TO_CLIENT = NetworkRegistry.newSimpleChannel(
            BuildDefine(MODID,"dimension_to_client"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    public static void register_to_player() {
        INSTANCE_TO_CLIENT.messageBuilder(DataPacket.class,3, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(DataPacket::new)
                .encoder(DataPacket::encode)
                .consumerMainThread(DataPacket::handle)
                .add();
    }


    public static <MSG> void setMsgToPlayer(MSG message, ServerPlayer player) {
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        //INSTANCE.send(PacketDistributor.ALL.noArg(), message);
        MSG_TO_CLIENT.send(PacketDistributor.PLAYER.with(() -> player), message);
    }




    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player == null) {
            System.err.println("Player is null, cannot send message.");
            return;
        }
        //INSTANCE.send(PacketDistributor.ALL.noArg(), message);
        INSTANCE_TO_CLIENT.send(PacketDistributor.PLAYER.with(() -> player), message);
    }


    public static <MSG> void sendToServer(MSG message) {
        INSTANCE_TO_SERVER.sendToServer(message);
    }




}
