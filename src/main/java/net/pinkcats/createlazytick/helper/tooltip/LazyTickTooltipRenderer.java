package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Channel.CLTChannel;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.Gui.mes;
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

        ToolTipStatus(tooltip);

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showModeTooltip()) {
            // 渲染目前懒加载模式
            tooltip.addAll(LazyTickMode.getDisplayComponents(dynamicValue, forcedValue, maxDelayTick));
        }

        if (control.createLazyTick$shouldRenderTier() && ClientConfig.showTierTooltip()) {
            // 渲染目前懒加载状态
            int currentInterval = control.createLazyTick$getCurrentSuperTick();
            //mes.debug(currentInterval);

            // 2. 计算百分比上限 (用于绘制紫色游标)
            // 优先取强制值，否则取动态上限。
            if (currentInterval < 1) currentInterval = 1;
            int limitPercent = (forcedValue > 0) ? forcedValue : dynamicValue;

            // 3. 渲染高级进度条
            tooltip.addAll(control.lazytick$getSyncedTier()
                    .getDisplayComponents(currentInterval, maxDelayTick, limitPercent));
        }

        String op = control.createLazyTick$getOwnerName();
        if (!op.isEmpty()) {
            // 渲染操作者
            tooltip.add(Component.translatable("createlazytick.tooltip.operator", op).withStyle(ChatFormatting.DARK_GRAY));
        }

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showDescriptionTooltip()) {
            tooltip.add(LazyTickMode.getModeDescription(dynamicValue, forcedValue));
        }

        List<Component> customInfo = control.createLazyTick$getCustomTooltipInfo();
        if (customInfo != null && !customInfo.isEmpty()) {
            // 渲染自定义文本
            tooltip.add(mes.spaces(3));
            tooltip.addAll(customInfo);
        }

        return currentTick;
    }


    public static void appendSimpleConfigInfo(Object be, List<Component> tooltip) {
        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(be);
        if (whiteItem != null) {
            ToolTipStatus(tooltip);
            if (whiteItem == LazyTickTooltipWhiteList.PIPE || whiteItem == LazyTickTooltipWhiteList.PUMP) {
                tooltip.add(Component.translatable("createlazytick.tooltip.fluid_system_delay", whiteItem.getMaxTick())
                        .withStyle(ChatFormatting.GRAY));
            } else if (whiteItem == LazyTickTooltipWhiteList.BELT) {
                tooltip.add(Component.translatable( "createlazytick.tooltip.belt_system_delay" , whiteItem.getMaxTick())
                        .withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("createlazytick.tooltip.global_config_note")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static void appendSnapshotInfo(CompoundTag tag, List<Component> tooltip, int maxDelayTick) {
        int dynamicValue = tag.contains("cltDynamic") ? tag.getInt("cltDynamic") : 100;
        int forcedValue = tag.contains("cltForced") ? tag.getInt("cltForced") : -1;
        int currentInterval = tag.contains("cltCurrentInterval") ? Math.max(1, tag.getInt("cltCurrentInterval")) : 1;
        String operator = tag.contains("cltOwner") ? tag.getString("cltOwner") : "";

        ToolTipStatus(tooltip);

        if (ClientConfig.showModeTooltip()) {
            tooltip.addAll(LazyTickMode.getDisplayComponents(dynamicValue, forcedValue, maxDelayTick));
        }

        if (ClientConfig.showTierTooltip()) {
            int limitPercent = forcedValue > 0 ? forcedValue : dynamicValue;
            tooltip.addAll(LazyTickTier.fromTicks(currentInterval, maxDelayTick)
                    .getDisplayComponents(currentInterval, maxDelayTick, limitPercent));
        }

        if (!operator.isEmpty()) {
            tooltip.add(Component.translatable("createlazytick.tooltip.operator", operator)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (ClientConfig.showDescriptionTooltip()) {
            tooltip.add(LazyTickMode.getModeDescription(dynamicValue, forcedValue));
        }
    }

    private static void ToolTipStatus(List<Component> tooltip) {
        if (tooltip.isEmpty()) {
            tooltip.add(mes.spaces(9));
            tooltip.add(Component.translatable("createlazytick.tooltip.status").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(mes.spaces(3));
            tooltip.add(Component.translatable("createlazytick.tooltip.status").withStyle(ChatFormatting.GRAY));
        }
    }
}
