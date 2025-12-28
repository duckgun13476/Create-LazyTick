package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Channel.CLTChannel;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

import java.util.List;

public class LazyTickTooltipHelper {
    private static final int frequent = 60;

    public static int appendLazyTickInfo(ISmartBlockEntityControl control, List<Component> tooltip,
                                         int currentTick, int maxDelayTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return currentTick;

        // Handle packet
        currentTick++;
        if (currentTick >= frequent) {
            currentTick = 0;

            int extraDataToSend = 0;
            CLTChannel.sendToServer(new ClockSyncPacket(
                    extraDataToSend,
                    control.CLT$getDimension().hashCode(),
                    control.CLT$getPos()
            ));
        }

        int stateId = control.createLazyTick$ControlState();

        if (tooltip.isEmpty()) {
            // 如果是第一行（置物台），加4个空格缩进，给左侧图标留位置
            tooltip.add(Component.literal("    LazyTick Status:").withStyle(ChatFormatting.GRAY));
        } else {
            // 如果不是第一行，先加个空行隔开
            tooltip.add(Component.literal("   "));
            tooltip.add(Component.literal("LazyTick Status:").withStyle(ChatFormatting.GRAY));
        }

        if (control.createLazyTick$shouldRenderMode()) {
            // 渲染目前懒加载模式
            tooltip.add(LazyTickMode.fromId(stateId).getDisplayComponent());
        }

        if (control.createLazyTick$shouldRenderTier() && stateId != 1) {
            // 渲染目前懒加载状态
            tooltip.add(control.lazytick$getSyncedTier().getDisplayComponent(maxDelayTick));
        }

        List<Component> customInfo = control.createLazyTick$getCustomTooltipInfo();
        if (customInfo != null && !customInfo.isEmpty()) {
            // 渲染自定义文本
            tooltip.addAll(customInfo);
        }

        String op = control.createLazyTick$getUserName();
        if (!op.isEmpty()) {
            // 渲染操作者
            tooltip.add(Component.literal(" 操作者: " + op).withStyle(ChatFormatting.DARK_GRAY));
        }

        return currentTick;
    }


    public static boolean shouldRender(Minecraft mc) {
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem;
    }
}
