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
    private static final ResourceLocation MAP_ICONS =
            new ResourceLocation("textures/map/map_icons.png");
    private static final ItemStack BOAT_STACK = new ItemStack(Items.OAK_BOAT);

    private WorldMapPlayerRenderer() {}

    public static void render(
            GuiGraphics guiGraphics,
            Font font,
            Player player,
            double offsetX,
            double offsetZ,
            double scale) {
        if (player == null) return;

        double playerWorldX = player.getX();
        double playerWorldZ = player.getZ();
        double pixelX = offsetX + playerWorldX * scale;
        double pixelZ = offsetZ + playerWorldZ * scale;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(pixelX, pixelZ, 0);
        if (player.getVehicle() instanceof Boat) renderBoat(pose, guiGraphics, player);
        else renderIcon(pose, guiGraphics, player);
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

    private static void renderIcon(PoseStack pose, GuiGraphics guiGraphics, Player player) {
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
                .vertex(matrix, -1f, 1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();
        consumer
                .vertex(matrix, 1f, 1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();
        consumer
                .vertex(matrix, 1f, -1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();
        consumer
                .vertex(matrix, -1f, -1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();
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
