package com.talhanation.recruits.client.gui.worldmap.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.compat.smallships.SmallShips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

public final class WorldMapPlayerRenderer {
    private static final ResourceLocation MAP_ICONS = ResourceLocation.parse("textures/map/map_icons.png");
    private static final ItemStack BOAT_STACK = new ItemStack(Items.OAK_BOAT);
    private static final float PLAYER_ARROW_BASE_SCALE = 0.55F;
    private static final float PLAYER_ARROW_MIN_SCALE = 0.18F;
    private static final float PLAYER_ARROW_MAX_SCALE = 0.55F;
    private static final int[][] PLAYER_ARROW_SPANS = {
            {0, 2, 23, 25}, {0, 4, 21, 25}, {0, 6, 19, 25}, {1, 8, 17, 24}, {1, 10, 15, 24},
            {2, 23, -1, -1}, {2, 23, -1, -1}, {3, 22, -1, -1}, {3, 22, -1, -1},
            {4, 21, -1, -1}, {4, 21, -1, -1}, {5, 20, -1, -1}, {5, 20, -1, -1},
            {6, 19, -1, -1}, {6, 19, -1, -1}, {7, 18, -1, -1}, {7, 18, -1, -1},
            {7, 18, -1, -1}, {8, 17, -1, -1}, {9, 16, -1, -1}, {9, 16, -1, -1},
            {10, 15, -1, -1}, {10, 15, -1, -1}, {10, 15, -1, -1}, {11, 14, -1, -1},
            {11, 14, -1, -1}, {12, 13, -1, -1}
    };

    private WorldMapPlayerRenderer() {}

    public static void render(
            GuiGraphics guiGraphics,
            Font font,
            Player player,
            double offsetX,
            double offsetZ,
            double scale,
            boolean usePlayerArrow) {
        if (player == null) return;

        double playerWorldX = player.getX();
        double playerWorldZ = player.getZ();
        double pixelX = offsetX + playerWorldX * scale;
        double pixelZ = offsetZ + playerWorldZ * scale;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(pixelX, pixelZ, 0);
        if (player.getVehicle() instanceof Boat) renderBoat(pose, guiGraphics, player);
        else renderIcon(pose, guiGraphics, player, scale, usePlayerArrow);
        pose.popPose();
        renderNameTag(guiGraphics, font, player, pixelX, pixelZ, scale);
    }

    private static void renderBoat(PoseStack pose, GuiGraphics guiGraphics, Player player) {
        float yaw = player.getYRot() % 360f;
        if (yaw < -180f) yaw += 360f;
        if (yaw >= 180f) yaw -= 360f;
        boolean flipX = yaw > 0;
        pose.pushPose();
        if (flipX) pose.scale(-1f, 1f, 1f);
        pose.scale(1.5f, 1.5f, 1.5f);
        Lighting.setupForFlatItems();
        ItemStack boat = BOAT_STACK;
        if (Main.isSmallShipsLoaded
                && player.getVehicle() != null
                && SmallShips.isSmallShip(player.getVehicle())) {
            boat = SmallShips.getSmallShipsItem();
        }
        RenderSystem.disableCull();
        guiGraphics.renderItem(boat, -8, -8);
        RenderSystem.enableCull();
        pose.popPose();
    }

    private static void renderIcon(
            PoseStack pose, GuiGraphics guiGraphics, Player player, double scale, boolean usePlayerArrow) {
        if (usePlayerArrow) {
            renderPlayerArrowIcon(pose, guiGraphics, player, scale);
            return;
        }
        renderVanillaIcon(pose, guiGraphics, player);
    }

    private static void renderVanillaIcon(PoseStack pose, GuiGraphics guiGraphics, Player player) {
        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));
        pose.scale(5.0f, 5.0f, 5.0f);
        int iconIndex = 0;
        float u0 = (iconIndex % 16) / 16f;
        float v0 = (iconIndex / 16) / 16f;
        float u1 = u0 + 1f / 16f;
        float v1 = v0 + 1f / 16f;
        guiGraphics.flush();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.text(MAP_ICONS));
        Matrix4f matrix = pose.last().pose();
        int light = 0xF000F0;
        int color = 0xFFFFFFFF;
        consumer
                .addVertex(matrix, -1f, 1f, 0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 0, 1)
                ;
        consumer
                .addVertex(matrix, 1f, 1f, 0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 0, 1)
                ;
        consumer
                .addVertex(matrix, 1f, -1f, 0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 0, 1)
                ;
        consumer
                .addVertex(matrix, -1f, -1f, 0f)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 0, 1)
                ;
    }

    private static void renderPlayerArrowIcon(
            PoseStack pose, GuiGraphics guiGraphics, Player player, double mapScale) {
        float arrowScale = getPlayerArrowScale(mapScale);

        pose.pushPose();
        pose.translate(0, 2.0F * arrowScale, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));
        pose.scale(arrowScale, arrowScale, 1.0F);
        renderPlayerArrowMask(guiGraphics, 0xE0000000);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));
        pose.scale(arrowScale, arrowScale, 1.0F);
        renderPlayerArrowMask(guiGraphics, 0xFF2BEA68);
        pose.popPose();
        guiGraphics.flush();
    }

    private static float getPlayerArrowScale(double mapScale) {
        float scale = (float) mapScale * PLAYER_ARROW_BASE_SCALE;
        return Math.max(PLAYER_ARROW_MIN_SCALE, Math.min(PLAYER_ARROW_MAX_SCALE, scale));
    }

    private static void renderPlayerArrowMask(GuiGraphics guiGraphics, int color) {
        for (int row = 0; row < PLAYER_ARROW_SPANS.length; row++) {
            int[] spans = PLAYER_ARROW_SPANS[row];
            drawPlayerArrowSpan(guiGraphics, spans[0], spans[1], row, color);
            if (spans[2] >= 0) drawPlayerArrowSpan(guiGraphics, spans[2], spans[3], row, color);
        }
    }

    private static void drawPlayerArrowSpan(GuiGraphics guiGraphics, int startX, int endX, int row, int color) {
        MapRenderUtil.fill(guiGraphics, startX - 13, row - 5, endX - 12, row - 4, color);
    }

    private static void renderNameTag(
            GuiGraphics guiGraphics,
            Font font,
            Player player,
            double pixelX,
            double pixelZ,
            double scale) {
        if (scale <= 1.5) return;

        String playerName = player.getName().getString();
        float textScale = (float) Math.min(1.0, scale / 1.25);
        int textWidth = font.width(playerName);
        int textHeight = font.lineHeight;
        guiGraphics.pose().pushPose();
        guiGraphics
                .pose()
                .translate(
                        pixelX - (textWidth * textScale) / 2.0,
                        pixelZ - (textHeight * textScale) / 2.0 - 10,
                        0);
        guiGraphics.pose().scale(textScale, textScale, 1.0f);
        guiGraphics.drawString(font, playerName, 0, 0, 0xFFFFFF, false);
        guiGraphics.pose().popPose();
    }
}
