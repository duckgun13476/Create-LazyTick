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
                    control.CLT$getDimension().location().toString(),
                    control.CLT$getPos()
            ));
        }

        int stateId = control.createLazyTick$ControlState();
        int dynamicValue = control.createLazyTick$getDynamicValue();
        int forcedValue = control.createLazyTick$getForcedValue();

        if (tooltip.isEmpty()) {
            // 如果是第一行（置物台），加4个空格缩进，给左侧图标留位置
            tooltip.add(Component.literal("        LazyTick Status:").withStyle(ChatFormatting.GRAY));
        } else {
            // 如果不是第一行，先加个空行隔开
            tooltip.add(Component.literal("   "));
            tooltip.add(Component.literal("LazyTick Status:").withStyle(ChatFormatting.GRAY));
        }

        if (control.createLazyTick$shouldRenderMode()) {
            // 渲染目前懒加载模式
            //tooltip.add(LazyTickMode.fromId(stateId).getDisplayComponent());
            tooltip.add(LazyTickMode.getDisplayComponent(dynamicValue, forcedValue, maxDelayTick));
        }

        if (control.createLazyTick$shouldRenderTier() && stateId != 1) {
            // 渲染目前懒加载状态
            tooltip.add(control.lazytick$getSyncedTier().getDisplayComponent(maxDelayTick));
        }

        String op = control.createLazyTick$getUserName();
        if (!op.isEmpty()) {
            // 渲染操作者
            tooltip.add(Component.literal(" 操作者: " + op).withStyle(ChatFormatting.DARK_GRAY));
        }

        List<Component> customInfo = control.createLazyTick$getCustomTooltipInfo();
        if (customInfo != null && !customInfo.isEmpty()) {
            // 渲染自定义文本
            tooltip.add(Component.literal("  "));
            tooltip.addAll(customInfo);
        }

        return currentTick;
    }


    public static void appendSimpleConfigInfo(Object be, List<Component> tooltip) {
        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(be);
        if (whiteItem != null) {
            if (tooltip.isEmpty()) {
                tooltip.add(Component.literal("        LazyTick Status:").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("   "));
                tooltip.add(Component.literal("LazyTick Status:").withStyle(ChatFormatting.GRAY));
            }

            tooltip.add(Component.literal("流体系统延迟刻(懒惰刻): " + whiteItem.getMaxTick())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("* 此为全局配置，不可单独调整")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }


    public static boolean shouldRender(Minecraft mc) {
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem;
    }
}
