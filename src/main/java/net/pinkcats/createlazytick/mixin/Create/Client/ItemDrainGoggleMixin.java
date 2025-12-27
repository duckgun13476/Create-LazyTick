package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Channel.CLTChannel;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.item.LazyTickClockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ItemDrainBlockEntity.class, remap = false)
public class ItemDrainGoggleMixin {

    @Unique
    private static final int createLazyTick$Frequent = 60;

    @Unique
    private int createLazyTick$tick = 0;

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void lazytick$appendDrainInfo(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem)) return;

        if ((Object) this instanceof ISmartBlockEntityControl control) {
            createLazyTick$tick++;
            if (createLazyTick$tick >= createLazyTick$Frequent) {
                createLazyTick$tick = 0;
                CLTChannel.sendToServer(new ClockSyncPacket(
                        mc.player.getModelName(),
                        control.CLT$getDimension().hashCode(),
                        control.CLT$getPos()
                ));
            }

            if (tooltip.isEmpty()) {
                tooltip.add(Component.literal("    LazyTick Status:").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("   "));
                tooltip.add(Component.literal("LazyTick Status:").withStyle(ChatFormatting.GRAY));
            }

            int state = control.createLazyTick$ControlState();
            if (state == 1) {
                tooltip.add(Component.literal(" [强制全速模式]").withStyle(ChatFormatting.DARK_PURPLE));
            } else if (state == 2) {
                tooltip.add(Component.literal(" [强制浅度休眠模式]").withStyle(ChatFormatting.YELLOW));
            } else if (state == 3) {
                tooltip.add(Component.literal(" [强制中度休眠模式]").withStyle(ChatFormatting.GOLD));
            } else if (state == 4) {
                tooltip.add(Component.literal(" [强制深度休眠模式]").withStyle(ChatFormatting.RED));
            } else if (state == 0) {
                tooltip.add(Component.literal(" [自动休眠模式]").withStyle(ChatFormatting.GRAY));
            }

            if (state != 1) {
                tooltip.add(control.lazytick$getSyncedTier().getDisplayComponent(Config.item_drain_delay_max));
            }

            // 5. 显示操作者
            String op = control.createLazyTick$getUserName();
            if (!op.isEmpty()) {
                tooltip.add(Component.literal(" 操作者: " + op).withStyle(ChatFormatting.DARK_GRAY));
            }

            // 6. 强制渲染
            cir.setReturnValue(true);
        }
    }
}