package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Channel.CLTChannel;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.helper.extraDataTool.ArmExtraDataTool;
import net.pinkcats.createlazytick.helper.extraDataTool.CrafterExtraDataTool;

import java.util.ArrayList;
import java.util.List;

public class LazyTickTooltipRenderer {
    private static long lastQueryTick = -1;
    private static BlockPos lastQueryPos = null;

    public static void requestState(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || pos == null) {
            return;
        }

        long currentGameTime = mc.level.getGameTime();
        if (currentGameTime % 10 != 0) {
            return;
        }
        if (currentGameTime == lastQueryTick && pos.equals(lastQueryPos)) {
            return;
        }

        lastQueryTick = currentGameTime;
        lastQueryPos = pos.immutable();
        CLTChannel.sendToServer(new ClockSyncPacket(pos));
    }

    public static int appendLazyTickInfo(ISmartBlockEntityControl control, List<Component> tooltip,
                                         int currentTick, int maxDelayTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return currentTick;
        }

        requestState(control.CLT$getPos());

        int dynamicValue = control.createLazyTick$getDynamicValue();
        int forcedValue = control.createLazyTick$getForcedValue();

        addTooltipStatus(tooltip);

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showModeTooltip()) {
            tooltip.addAll(LazyTickMode.getDisplayComponents(dynamicValue, forcedValue, maxDelayTick));
        }

        if (control.createLazyTick$shouldRenderTier() && ClientConfig.showTierTooltip()) {
            int currentInterval = Math.max(1, control.createLazyTick$getCurrentSuperTick());
            int limitPercent = forcedValue > 0 ? forcedValue : dynamicValue;
            tooltip.addAll(control.lazytick$getSyncedTier()
                    .getDisplayComponents(currentInterval, maxDelayTick, limitPercent));
        }

        String operator = control.createLazyTick$getOwnerName();
        if (!operator.isEmpty()) {
            tooltip.add(Component.translatable("createlazytick.tooltip.operator", operator)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (control.createLazyTick$shouldRenderMode() && ClientConfig.showDescriptionTooltip()) {
            tooltip.add(LazyTickMode.getModeDescription(dynamicValue, forcedValue));
        }

        List<Component> customInfo = control.createLazyTick$getCustomTooltipInfo();
        if (customInfo != null && !customInfo.isEmpty()) {
            tooltip.add(mes.spaces(3));
            tooltip.addAll(customInfo);
        }

        return currentTick;
    }

    public static void appendSimpleConfigInfo(Object be, List<Component> tooltip) {
        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(be);
        if (whiteItem == null) {
            return;
        }

        addTooltipStatus(tooltip);
        if (whiteItem == LazyTickTooltipWhiteList.PIPE || whiteItem == LazyTickTooltipWhiteList.PUMP) {
            tooltip.add(Component.translatable("createlazytick.tooltip.fluid_system_delay", whiteItem.getMaxTick())
                    .withStyle(ChatFormatting.GRAY));
        } else if (whiteItem == LazyTickTooltipWhiteList.BELT) {
            tooltip.add(Component.translatable("createlazytick.tooltip.belt_system_delay", whiteItem.getMaxTick())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("createlazytick.tooltip.global_config_note")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void appendSnapshotInfo(LazyTickTooltipWhiteList whiteItem, CompoundTag tag,
                                          List<Component> tooltip, int maxDelayTick) {
        int dynamicValue = tag.contains("cltDynamic") ? tag.getInt("cltDynamic") : 100;
        int forcedValue = tag.contains("cltForced") ? tag.getInt("cltForced") : -1;
        int currentInterval = tag.contains("cltCurrentInterval") ? Math.max(1, tag.getInt("cltCurrentInterval")) : 1;
        String operator = tag.contains("cltOwner") ? tag.getString("cltOwner") : "";
        int extraData = tag.contains("cltExtraData") ? tag.getInt("cltExtraData") : 0;

        boolean renderMode = true;
        boolean renderTier = true;
        if (whiteItem == LazyTickTooltipWhiteList.ARM) {
            boolean ignore = ArmExtraDataTool.unpackIgnore(extraData);
            renderMode = !ignore;
            renderTier = !ignore;
        }

        addTooltipStatus(tooltip);

        if (ClientConfig.showModeTooltip() && renderMode) {
            tooltip.addAll(LazyTickMode.getDisplayComponents(dynamicValue, forcedValue, maxDelayTick));
        }

        if (ClientConfig.showTierTooltip() && renderTier) {
            int limitPercent = forcedValue > 0 ? forcedValue : dynamicValue;
            tooltip.addAll(LazyTickTier.fromTicks(currentInterval, maxDelayTick)
                    .getDisplayComponents(currentInterval, maxDelayTick, limitPercent));
        }

        if (!operator.isEmpty()) {
            tooltip.add(Component.translatable("createlazytick.tooltip.operator", operator)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (ClientConfig.showDescriptionTooltip() && renderMode) {
            tooltip.add(LazyTickMode.getModeDescription(dynamicValue, forcedValue));
        }

        List<Component> customInfo = buildSnapshotCustomInfo(whiteItem, extraData);
        if (!customInfo.isEmpty()) {
            tooltip.add(mes.spaces(3));
            tooltip.addAll(customInfo);
        }
    }

    private static List<Component> buildSnapshotCustomInfo(LazyTickTooltipWhiteList whiteItem, int extraData) {
        List<Component> tooltip = new ArrayList<>();

        if (whiteItem == LazyTickTooltipWhiteList.ARM) {
            boolean ignore = ArmExtraDataTool.unpackIgnore(extraData);
            boolean weak = ArmExtraDataTool.unpackWeak(extraData);

            if (ignore) {
                tooltip.add(Component.translatable("createlazytick.arm.config_exemption_full_speed")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("createlazytick.arm.ignore_list_disabled")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("createlazytick.arm.cannot_change_interval")
                        .withStyle(ChatFormatting.GOLD));
            } else if (weak) {
                tooltip.add(Component.translatable("createlazytick.arm.config_exemption_weak")
                        .withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.translatable("createlazytick.arm.weak_list_shortened")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("createlazytick.arm.can_change_by_force")
                        .withStyle(ChatFormatting.GRAY));
            }

            return tooltip;
        }

        if (whiteItem == LazyTickTooltipWhiteList.CRAFTER) {
            boolean isPowered = CrafterExtraDataTool.unpackIsPowered(extraData);
            boolean isInWindow = CrafterExtraDataTool.unpackInWindow(extraData);
            boolean isDelayForced = CrafterExtraDataTool.unpackIsDelayForced(extraData);

            if (isPowered) {
                tooltip.add(Component.translatable("createlazytick.crafter.redstone_powered")
                        .withStyle(ChatFormatting.RED));
                if (!isDelayForced) {
                    if (isInWindow) {
                        tooltip.add(Component.translatable("createlazytick.crafter.full_speed_window")
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        tooltip.add(Component.translatable("createlazytick.crafter.idle_too_long")
                                .withStyle(ChatFormatting.RED));
                    }
                } else {
                    tooltip.add(Component.translatable("createlazytick.crafter.forced_control")
                            .withStyle(ChatFormatting.GRAY));
                }
            } else {
                tooltip.add(Component.translatable("createlazytick.crafter.redstone_unpowered")
                        .withStyle(ChatFormatting.DARK_GRAY));
                if (isInWindow) {
                    if (isDelayForced) {
                        tooltip.add(Component.translatable("createlazytick.crafter.forced_control")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.translatable("createlazytick.crafter.fast_speed_window")
                                .withStyle(ChatFormatting.GREEN));
                    }
                }
            }

            tooltip.add(Component.translatable("createlazytick.crafter.no_delay_full_slots")
                    .withStyle(ChatFormatting.GRAY));
        }

        return tooltip;
    }

    private static void addTooltipStatus(List<Component> tooltip) {
        if (tooltip.isEmpty()) {
            tooltip.add(mes.spaces(9));
            tooltip.add(Component.translatable("createlazytick.tooltip.status").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(mes.spaces(3));
        tooltip.add(Component.translatable("createlazytick.tooltip.status").withStyle(ChatFormatting.GRAY));
    }
}
