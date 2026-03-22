package net.pinkcats.createlazytick.client;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.pinkcats.createlazytick.Channel.LazyTickClientStateCache;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;

import java.util.ArrayList;
import java.util.List;

public class LazyTickTooltipOverlay {
    private static final int GOGGLES_ICON_OFFSET_X = 10;
    private static final int GOGGLES_ICON_OFFSET_Y = -16;
    private static final int TOOLTIP_ANCHOR_SHIFT_X = 16;
    private static final int TOOLTIP_ANCHOR_SHIFT_Y = -16;
    private static final int TOOLTIP_TEXT_OFFSET_Y = -2;
    private static final int TOOLTIP_SCREEN_PADDING = 20;
    public static final LayeredDraw.Layer OVERLAY = (guiGraphics, deltaTracker) -> renderOverlay(guiGraphics);

    private static int hoverTicks = 0;
    private static BlockPos lastHovered = null;

    public static void renderOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            hoverTicks = 0;
            lastHovered = null;
            return;
        }
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            hoverTicks = 0;
            lastHovered = null;
            return;
        }
        if (!isActuallyWearingGoggles(mc) || !LazyTickTooltipTool.shouldRender(mc)) {
            hoverTicks = 0;
            lastHovered = null;
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            hoverTicks = 0;
            lastHovered = null;
            return;
        }

        BlockPos originalPos = hit.getBlockPos();
        BlockPos targetPos = GoggleOverlayRenderer.proxiedOverlayPosition(mc.level, originalPos);
        BlockEntity blockEntity = mc.level.getBlockEntity(targetPos);
        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(blockEntity);
        if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
            LazyTickDepotDebug.logBlockEntity(mc, "overlay_render", targetPos, blockEntity,
                    "entered overlay, originalPos=" + originalPos
                            + ", targetPos=" + targetPos
                            + ", isSmartControl=" + (blockEntity instanceof ISmartBlockEntityControl));
        }
        List<Component> tooltip = buildTooltip(mc, targetPos, blockEntity, whiteItem);
        if (tooltip.isEmpty()) {
            if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                LazyTickDepotDebug.logBlockEntity(mc, "overlay_render", targetPos, blockEntity,
                        "tooltip empty after build");
            }
            hoverTicks = 0;
            lastHovered = null;
            return;
        }

        hoverTicks = targetPos.equals(lastHovered) ? hoverTicks + 1 : 1;
        lastHovered = targetPos;
        if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
            LazyTickDepotDebug.logBlockEntity(mc, "overlay_render", targetPos, blockEntity,
                    "rendering tooltip, size=" + tooltip.size() + ", hoverTicks=" + hoverTicks);
        }
        renderTooltip(guiGraphics, mc, tooltip);
    }

    private static List<Component> buildTooltip(Minecraft mc, BlockPos targetPos, BlockEntity blockEntity,
                                                LazyTickTooltipWhiteList whiteItem) {
        List<Component> tooltip = new ArrayList<>();
        if (blockEntity == null) {
            return tooltip;
        }

        if (whiteItem == null) {
            return tooltip;
        }

        if (whiteItem == LazyTickTooltipWhiteList.PIPE
                || whiteItem == LazyTickTooltipWhiteList.PUMP
                || whiteItem == LazyTickTooltipWhiteList.BELT) {
            LazyTickTooltipRenderer.appendSimpleConfigInfo(blockEntity, tooltip);
            return tooltip;
        }

        LazyTickTooltipRenderer.requestState(targetPos);

        CompoundTag syncedTag = LazyTickClientStateCache.get(mc.level.dimension().location().toString(), targetPos);
        if (syncedTag != null && hasLazyTickState(syncedTag)) {
            LazyTickTooltipRenderer.appendSnapshotInfo(whiteItem, syncedTag, tooltip, whiteItem.getMaxTick());
            if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                LazyTickDepotDebug.logBlockEntity(mc, "overlay_build", targetPos, blockEntity,
                        "cache path used, keys=" + syncedTag.getAllKeys() + ", size=" + tooltip.size());
            }
            if (!tooltip.isEmpty()) {
                return tooltip;
            }
        }

        ISmartBlockEntityControl control = blockEntity instanceof ISmartBlockEntityControl smart
                ? smart
                : SmartLazyTickStateHelper.control(blockEntity);
        if (control != null) {
            LazyTickTooltipRenderer.appendLazyTickInfo(control, tooltip, 0, whiteItem.getMaxTick());
            if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                LazyTickDepotDebug.logBlockEntity(mc, "overlay_build", targetPos, blockEntity,
                        "control path used, size=" + tooltip.size());
            }
            if (!tooltip.isEmpty()) {
                return tooltip;
            }
        }

        CompoundTag tag = blockEntity.saveWithoutMetadata(mc.level.registryAccess());
        LazyTickTooltipRenderer.appendSnapshotInfo(whiteItem, tag, tooltip, whiteItem.getMaxTick());
        if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
            LazyTickDepotDebug.logBlockEntity(mc, "overlay_build", targetPos, blockEntity,
                    "snapshot path used, keys=" + tag.getAllKeys() + ", size=" + tooltip.size());
        }
        if (!tooltip.isEmpty()) {
            return tooltip;
        }

        if (blockEntity instanceof IHaveGoggleInformation goggleInformation) {
            boolean added = goggleInformation.addToGoggleTooltip(tooltip, mc.player != null && mc.player.isShiftKeyDown());
            if (whiteItem == LazyTickTooltipWhiteList.DEPOT) {
                LazyTickDepotDebug.logBlockEntity(mc, "overlay_build", targetPos, blockEntity,
                        "goggle tooltip fallback used, added=" + added + ", size=" + tooltip.size());
            }
        }

        return tooltip;
    }

    private static void renderTooltip(GuiGraphics guiGraphics, Minecraft mc, List<Component> tooltip) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int maxTooltipWidth = Math.min(240, Math.max(180, width - 40));

        int tooltipTextWidth = 0;
        int tooltipLineCount = 0;
        for (FormattedText textLine : tooltip) {
            int lineWidth = mc.font.width(textLine);
            tooltipTextWidth = Math.max(tooltipTextWidth, Math.min(lineWidth, maxTooltipWidth));
            int wrappedLines = Math.max(1, Mth.ceil((float) lineWidth / (float) maxTooltipWidth));
            tooltipLineCount += wrappedLines;
        }

        int tooltipHeight = 8;
        if (tooltipLineCount > 1) {
            tooltipHeight += 2;
            tooltipHeight += (tooltipLineCount - 1) * 10;
        }

        int overlayOffsetX = AllConfigs.client().overlayOffsetX.get();
        int overlayOffsetY = AllConfigs.client().overlayOffsetY.get();
        int desiredPosX = width / 2 + overlayOffsetX + TOOLTIP_ANCHOR_SHIFT_X;
        int desiredPosY = height / 2 + overlayOffsetY + TOOLTIP_ANCHOR_SHIFT_Y;
        int maxPosX = width - tooltipTextWidth - TOOLTIP_SCREEN_PADDING;
        int maxPosY = height - tooltipHeight - TOOLTIP_SCREEN_PADDING;
        int posX = Mth.clamp(desiredPosX, TOOLTIP_SCREEN_PADDING, Math.max(TOOLTIP_SCREEN_PADDING, maxPosX));
        int posY = Mth.clamp(desiredPosY, TOOLTIP_SCREEN_PADDING, Math.max(TOOLTIP_SCREEN_PADDING, maxPosY));

        float fade = Mth.clamp(hoverTicks / 24f, 0, 1);
        boolean useCustom = AllConfigs.client().overlayCustomColor.get();
        Color colorBackground = useCustom
                ? new Color(AllConfigs.client().overlayBackgroundColor.get())
                : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(.75f);
        Color colorBorderTop = useCustom
                ? new Color(AllConfigs.client().overlayBorderColorTop.get())
                : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
        Color colorBorderBot = useCustom
                ? new Color(AllConfigs.client().overlayBorderColorBot.get())
                : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

        guiGraphics.pose().pushPose();
        if (fade < 1) {
            guiGraphics.pose().translate(Math.pow(1 - fade, 3) * Math.signum(overlayOffsetX + .5f) * 8, 0, 0);
            colorBackground.scaleAlpha(fade);
            colorBorderTop.scaleAlpha(fade);
            colorBorderBot.scaleAlpha(fade);
        }

        if (LazyTickDepotDebug.enabled()) {
            LazyTickDepotDebug.log(mc, "overlay_draw",
                    "posX=" + posX + ", posY=" + posY
                            + ", width=" + width + ", height=" + height
                            + ", tooltipWidth=" + tooltipTextWidth + ", tooltipHeight=" + tooltipHeight);
        }

        int tooltipTextY = posY + TOOLTIP_TEXT_OFFSET_Y;
        RemovedGuiUtils.drawHoveringText(
                guiGraphics,
                tooltip,
                posX,
                tooltipTextY,
                width,
                height,
                maxTooltipWidth,
                colorBackground.getRGB(),
                colorBorderTop.getRGB(),
                colorBorderBot.getRGB(),
                mc.font
        );

        // Draw icon after tooltip so it always stays on top.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        ItemStack icon = AllItems.GOGGLES.asStack();
        guiGraphics.renderItem(icon, posX + GOGGLES_ICON_OFFSET_X, posY + GOGGLES_ICON_OFFSET_Y);
        guiGraphics.pose().popPose();
        guiGraphics.pose().popPose();
    }

    private static boolean isActuallyWearingGoggles(Minecraft mc) {
        return AllItems.GOGGLES.isIn(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
    }

    private static boolean hasLazyTickState(CompoundTag tag) {
        return tag.contains("cltCurrentInterval")
                || tag.contains("cltDynamic")
                || tag.contains("cltForced")
                || tag.contains("cltOwner")
                || tag.contains("cltExtraData")
                || tag.contains("cltTier");
    }
}
