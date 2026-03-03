package net.pinkcats.NutUI.menu.architect.Helper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.pinkcats.NutUI.menu.Nutprovider;

public class MenuHelper {

    // Auto Create Menu and Sync
    public static void CreateNutMenu(Player player, BlockPos pos,ResourceLocation MenuID){
        CreateNutMenu(player, pos, MenuID, null);
    }

    public static void CreateNutMenu(Player player, BlockPos pos, ResourceLocation MenuID, Nutprovider.MenuBuilder menuBuilder) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new Nutprovider(pos, MenuID, menuBuilder),
                    buf -> {
                        // RegistryFriendlyByteBuf
                        buf.writeBlockPos(pos);
                        buf.writeResourceLocation(MenuID);
                    }
            );
        }
    }
}
