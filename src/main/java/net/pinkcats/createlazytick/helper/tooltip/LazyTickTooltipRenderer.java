package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Channel.CLTChannel;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ClientConfig;

import java.util.List;
//需要翻译文本
public class LazyTickTooltipRenderer {
    // 记录上一次发送的时间，防止同一 tick 内发多次
    private static long lastQueryTick = -1;

    public static int appendLazyTickInfo(ISmartBlockEntityControl control, List<Component> tooltip,
                                         int currentTick, int maxDelayTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return currentTick;

        long currentGameTime = mc.level.getGameTime();

        // 增加 && currentGameTime != lastQueryTick 判断
        if (currentGameTime % 10 == 0 && currentGameTime != lastQueryTick) {
            // 记录当前时间，锁住这一 tick 后续的帧
            // 使用新的构造函数 (isQuery = true)
            lastQueryTick = currentGameTime;

            //System.out.println("new (Throttled)"); // 这样应该就只输出一次了
            CLTChannel.sendToServer(new ClockSyncPacket(control.CLT$getPos()));
        }

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

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showModeTooltip()) {
            // 渲染目前懒加载模式
            tooltip.addAll(LazyTickMode.getDisplayComponents(dynamicValue, forcedValue, maxDelayTick));
        }

        if (control.createLazyTick$shouldRenderTier() && ClientConfig.showTierTooltip()) {
            // 渲染目前懒加载状态
            int currentInterval = control.createLazyTick$getLazyTickInterval();

            // 2. 计算百分比上限 (用于绘制紫色游标)
            // 优先取强制值，否则取动态上限。
            if (currentInterval < 1) currentInterval = 1;
            int limitPercent = (forcedValue > 0) ? forcedValue : dynamicValue;

            // 3. 渲染高级进度条
            tooltip.addAll(control.lazytick$getSyncedTier()
                    .getDisplayComponents(currentInterval, maxDelayTick, limitPercent));
        }

        String op = control.createLazyTick$getUserName();
        if (!op.isEmpty()) {
            // 渲染操作者
            tooltip.add(Component.literal(" 操作者: " + op).withStyle(ChatFormatting.DARK_GRAY));
        }

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showDescriptionTooltip()) {
            tooltip.add(LazyTickMode.getModeDescription(dynamicValue, forcedValue));
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
}
