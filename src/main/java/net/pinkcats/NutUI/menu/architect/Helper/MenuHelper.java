package net.pinkcats.NutUI.menu.architect.Helper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import net.pinkcats.NutUI.menu.Nutprovider;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.Nut_Menu_ID;

public class MenuHelper {

    // Auto Create Menu and Sync
    public static void CreateNutMenu(Player player, BlockPos pos,ResourceLocation MenuID){
        NetworkHooks.openScreen(
                (ServerPlayer) player,
                new Nutprovider(pos, MenuID),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeResourceLocation(MenuID);
                }
        );
    }


}
