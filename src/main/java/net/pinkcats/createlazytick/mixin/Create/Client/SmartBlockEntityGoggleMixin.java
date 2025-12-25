package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.item.LazyTickClockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityGoggleMixin implements IHaveHoveringInformation {

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (!(mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem)) return false;

        if ((Object) this instanceof ItemDrainBlockEntity) return false;

        if (this instanceof ISmartBlockEntityControl control) {

            System.out.println("  " + control.createLazyTick$ControlState());
            // [UI修复] 防止图标遮挡
            if (tooltip.isEmpty()) {
                // 如果是第一行（置物台），加4个空格缩进，给左侧图标留位置
                tooltip.add(Component.literal("    LazyTick Status:").withStyle(ChatFormatting.GRAY));
            } else {
                // 如果不是第一行，先加个空行隔开
                tooltip.add(Component.literal("   "));
                tooltip.add(Component.literal("LazyTick Status:").withStyle(ChatFormatting.GRAY));
            }

            if (control.createLazyTick$ControlState() ==1) {
                tooltip.add(Component.literal(" [强制全速模式]").withStyle(ChatFormatting.RED));

            } else if (control.createLazyTick$ControlState() == 2) {
                tooltip.add(Component.literal(" [浅度休眠模式]").withStyle(ChatFormatting.RED));
            } else if (control.createLazyTick$ControlState() == 3) {
                tooltip.add(Component.literal(" [中度休眠模式]").withStyle(ChatFormatting.RED));
            } else if (control.createLazyTick$ControlState() == 0) {
                tooltip.add(Component.literal(" [深度休眠模式]").withStyle(ChatFormatting.RED));
            }

            if (control.createLazyTick$ControlState() != 1) {
                int maxTick = lazytick$getMaxTickFor((SmartBlockEntity) (Object) this);
                tooltip.add(control.lazytick$getSyncedTier().getDisplayComponent(maxTick));
            }

            String op = control.createLazyTick$getUserName();
            if (!op.isEmpty()) {
                tooltip.add(Component.literal(" 操作者: " + op).withStyle(ChatFormatting.DARK_GRAY));
            }

            return true;
        }

        return false;
    }

    @Unique
    private int lazytick$getMaxTickFor(SmartBlockEntity be) {
        if (be instanceof FunnelBlockEntity) return Config.funnel_delay_max;
        if (be instanceof DepotBlockEntity) return Config.depot_delay_max;
        return 60;
    }
}