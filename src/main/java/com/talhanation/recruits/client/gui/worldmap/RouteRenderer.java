package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.world.RecruitsRoute;
import com.talhanation.recruits.world.RecruitsRoute.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.List;

public class RouteRenderer {

    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");

    private static final int COLOR_NORMAL      = 0xFFFFFFFF; // white
    private static final int COLOR_NOT_LOADED  = 0xFFFF4444; // red
    private static final int ICON_INDEX        = 6;

    // -------------------------------------------------------------------------

    public static void renderRoute(GuiGraphics guiGraphics, RecruitsRoute route,
                                   double offsetX, double offsetZ, double scale) {
        renderRoute(guiGraphics, route, offsetX, offsetZ, scale, null, -1);
    }

    public static void renderRoute(GuiGraphics guiGraphics, RecruitsRoute route,
                                   double offsetX, double offsetZ, double scale,
                                   @Nullable Waypoint draggingWaypoint, int dragInsertIndex) {
        if (route == null || route.getWaypoints().isEmpty()) return;

        List<Waypoint> waypoints = route.getWaypoints();

        renderLines(guiGraphics, waypoints, offsetX, offsetZ, scale, draggingWaypoint);
        renderInsertionIndicator(guiGraphics, waypoints, offsetX, offsetZ, scale,
                draggingWaypoint, dragInsertIndex);

        for (int i = 0; i < waypoints.size(); i++) {
            boolean isDragging = waypoints.get(i) == draggingWaypoint;
            renderWaypointIcon(guiGraphics, waypoints.get(i), i + 1, offsetX, offsetZ, scale, 0xFF, isDragging);
        }
    }

    public static void renderDragGhost(GuiGraphics guiGraphics, Waypoint waypoint, int mouseX, int mouseY) {
        if (waypoint == null) return;
        renderIconAt(guiGraphics, mouseX, mouseY, ICON_INDEX, COLOR_NORMAL, 0xFF);
        if (waypoint.getAction() != null) {
            String label = waypoint.getAction().toString();
            int textWidth = Minecraft.getInstance().font.width(label);
            guiGraphics.drawString(Minecraft.getInstance().font, label,
                    mouseX - textWidth / 2, mouseY + 6, COLOR_NORMAL, false);
        }
    }

    // -------------------------------------------------------------------------

    private static void renderLines(GuiGraphics guiGraphics, List<Waypoint> waypoints,
                                    double offsetX, double offsetZ, double scale,
                                    @Nullable Waypoint draggingWaypoint) {
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Waypoint a = waypoints.get(i);
            Waypoint b = waypoints.get(i + 1);
            if (a == draggingWaypoint || b == draggingWaypoint) continue;
            int x1 = (int)(offsetX + a.getPosition().getX() * scale);
            int z1 = (int)(offsetZ + a.getPosition().getZ() * scale);
            int x2 = (int)(offsetX + b.getPosition().getX() * scale);
            int z2 = (int)(offsetZ + b.getPosition().getZ() * scale);
            drawLine(guiGraphics, x1, z1, x2, z2, 0xAAFFFFFF);
        }
    }

    private static void renderInsertionIndicator(GuiGraphics guiGraphics, List<Waypoint> waypoints,
                                                  double offsetX, double offsetZ, double scale,
                                                  @Nullable Waypoint draggingWaypoint, int insertIndex) {
        if (draggingWaypoint == null || insertIndex < 0) return;

        java.util.List<Waypoint> without = new java.util.ArrayList<>(waypoints);
        without.remove(draggingWaypoint);

        int clampedIdx = Math.max(0, Math.min(insertIndex, without.size()));
        Waypoint prev = clampedIdx > 0              ? without.get(clampedIdx - 1) : null;
        Waypoint next = clampedIdx < without.size() ? without.get(clampedIdx)     : null;

        if (prev != null) {
            drawLine(guiGraphics,
                    (int)(offsetX + prev.getPosition().getX() * scale),
                    (int)(offsetZ + prev.getPosition().getZ() * scale),
                    (int)(offsetX + draggingWaypoint.getPosition().getX() * scale),
                    (int)(offsetZ + draggingWaypoint.getPosition().getZ() * scale),
                    0xFF00FFFF);
        }
        if (next != null) {
            drawLine(guiGraphics,
                    (int)(offsetX + draggingWaypoint.getPosition().getX() * scale),
                    (int)(offsetZ + draggingWaypoint.getPosition().getZ() * scale),
                    (int)(offsetX + next.getPosition().getX() * scale),
                    (int)(offsetZ + next.getPosition().getZ() * scale),
                    0xFF00FFFF);
        }
    }

    private static void renderWaypointIcon(GuiGraphics guiGraphics, Waypoint waypoint, int number,
                                           double offsetX, double offsetZ, double scale, int alpha,
                                           boolean isDragging) {
        int pixelX = (int)(offsetX + waypoint.getPosition().getX() * scale);
        int pixelZ = (int)(offsetZ + waypoint.getPosition().getZ() * scale);

        boolean loaded = isChunkLoaded(waypoint);
        int color = loaded ? COLOR_NORMAL : COLOR_NOT_LOADED;
        int argb  = (alpha << 24) | (color & 0x00FFFFFF);

        renderIconAt(guiGraphics, pixelX, pixelZ, ICON_INDEX, argb, alpha);

        // Number label
        String numStr = String.valueOf(number);
        int textWidth = Minecraft.getInstance().font.width(numStr);
        guiGraphics.drawString(Minecraft.getInstance().font, numStr,
                pixelX - textWidth / 2, pixelZ - 10, argb, false);

        // Status / action label below icon, only when zoomed in and not being dragged
        if (scale > 2.0 && !isDragging) {
            String label = null;
            if (!loaded) {
                label = "not loaded";
            } else if (waypoint.getAction() != null) {
                label = waypoint.getAction().toString();
            }
            if (label != null) {
                int labelW = Minecraft.getInstance().font.width(label);
                guiGraphics.drawString(Minecraft.getInstance().font, label,
                        pixelX - labelW / 2, pixelZ + 5, argb, false);
            }
        }
    }

    private static boolean isChunkLoaded(Waypoint waypoint) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        ChunkPos chunk = new ChunkPos(
                waypoint.getPosition().getX() >> 4,
                waypoint.getPosition().getZ() >> 4);
        // Must be both explored on the map AND currently loaded in the level
        // so that surface Y can be resolved accurately when patrolling starts.
        if (!ChunkTileManager.getInstance().isChunkExplored(chunk)) return false;
        return level.getChunkSource().getChunk(chunk.x, chunk.z, false) != null;
    }

    private static void renderIconAt(GuiGraphics guiGraphics, int pixelX, int pixelZ,
                                     int iconIndex, int color, int alpha) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(pixelX, pixelZ, 0);
        pose.scale(3.0f, 3.0f, 3.0f);

        float u0 = (iconIndex % 16) / 16f;
        float v0 = (iconIndex / 16) / 16f;
        float u1 = u0 + 1f / 16f;
        float v1 = v0 + 1f / 16f;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b =  color        & 0xFF;

        guiGraphics.flush();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.text(MAP_ICONS));
        Matrix4f matrix = pose.last().pose();
        int light = 0xF000F0;
        consumer.vertex(matrix, -1f,  1f, 0f).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix,  1f,  1f, 0f).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix,  1f, -1f, 0f).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, -1f, -1f, 0f).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        guiGraphics.flush();

        pose.popPose();
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 <  dx) { err += dx; y1 += sy; }
        }
    }

    // -------------------------------------------------------------------------

    @Nullable
    public static Waypoint getWaypointAt(RecruitsRoute route, double mouseX, double mouseY,
                                         double offsetX, double offsetZ, double scale) {
        if (route == null) return null;
        int hitRadius = Math.max(5, (int)(8 * scale / 2.0));
        for (Waypoint wp : route.getWaypoints()) {
            int px = (int)(offsetX + wp.getPosition().getX() * scale);
            int pz = (int)(offsetZ + wp.getPosition().getZ() * scale);
            if (Math.abs(mouseX - px) <= hitRadius && Math.abs(mouseY - pz) <= hitRadius) return wp;
        }
        return null;
    }

    public static int computeInsertIndex(RecruitsRoute route, Waypoint dragging,
                                         double mouseX, double mouseY,
                                         double offsetX, double offsetZ, double scale) {
        if (route == null) return 0;

        List<Waypoint> without = new java.util.ArrayList<>(route.getWaypoints());
        without.remove(dragging);
        if (without.isEmpty()) return 0;

        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;

        double fx = offsetX + without.get(0).getPosition().getX() * scale;
        double fz = offsetZ + without.get(0).getPosition().getZ() * scale;
        double d = Math.hypot(mouseX - fx, mouseY - fz);
        double bias = (mouseX < fx) ? 0 : 20;
        if (d + bias < bestDist) { bestDist = d + bias; bestIndex = 0; }

        for (int i = 0; i < without.size() - 1; i++) {
            double mx = (offsetX + without.get(i).getPosition().getX() * scale
                       + offsetX + without.get(i+1).getPosition().getX() * scale) / 2.0;
            double mz = (offsetZ + without.get(i).getPosition().getZ() * scale
                       + offsetZ + without.get(i+1).getPosition().getZ() * scale) / 2.0;
            d = Math.hypot(mouseX - mx, mouseY - mz);
            if (d < bestDist) { bestDist = d; bestIndex = i + 1; }
        }

        double lx = offsetX + without.get(without.size()-1).getPosition().getX() * scale;
        double lz = offsetZ + without.get(without.size()-1).getPosition().getZ() * scale;
        d = Math.hypot(mouseX - lx, mouseY - lz);
        bias = (mouseX > lx) ? 0 : 20;
        if (d + bias < bestDist) bestIndex = without.size();

        return bestIndex;
    }
}
