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
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;

import java.util.ArrayList;
import java.util.List;

public class LazyTickTooltipOverlay {
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

        if (blockEntity instanceof ISmartBlockEntityControl control) {
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
        LazyTickTooltipRenderer.appendSnapshotInfo(tag, tooltip, whiteItem.getMaxTick());
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
        int tooltipTextWidth = 0;
        for (FormattedText textLine : tooltip) {
            tooltipTextWidth = Math.max(tooltipTextWidth, mc.font.width(textLine));
        }

        int tooltipHeight = 8;
        if (tooltip.size() > 1) {
            tooltipHeight += 2;
            tooltipHeight += (tooltip.size() - 1) * 10;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int posX = width / 2 + LazyTickCreateOverlaySuppressor.getOverlayOffsetX();
        int posY = height / 2 + LazyTickCreateOverlaySuppressor.getOverlayOffsetY();

        posX = Math.min(posX, width - tooltipTextWidth - 20);
        posY = Math.min(posY, height - tooltipHeight - 20);

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
            guiGraphics.pose().translate(Math.pow(1 - fade, 3) * Math.signum(LazyTickCreateOverlaySuppressor.getOverlayOffsetX() + .5f) * 8, 0, 0);
            colorBackground.scaleAlpha(fade);
            colorBorderTop.scaleAlpha(fade);
            colorBorderBot.scaleAlpha(fade);
        }

        ItemStack icon = new ItemStack(LazyTickItem.CLOCK.get());
        guiGraphics.renderItem(icon, posX + 2, posY - 18);
        if (LazyTickDepotDebug.enabled()) {
            LazyTickDepotDebug.log(mc, "overlay_draw",
                    "posX=" + posX + ", posY=" + posY
                            + ", width=" + width + ", height=" + height
                            + ", tooltipWidth=" + tooltipTextWidth + ", tooltipHeight=" + tooltipHeight);
        }

        RemovedGuiUtils.drawHoveringText(
                guiGraphics,
                tooltip,
                posX,
                posY,
                width,
                height,
                -1,
                colorBackground.getRGB(),
                colorBorderTop.getRGB(),
                colorBorderBot.getRGB(),
                mc.font
        );
        guiGraphics.pose().popPose();
    }

    private static boolean isActuallyWearingGoggles(Minecraft mc) {
        return AllItems.GOGGLES.isIn(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
    }
}
