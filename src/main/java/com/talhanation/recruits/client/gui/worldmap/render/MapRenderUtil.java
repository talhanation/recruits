package com.talhanation.recruits.client.gui.worldmap.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public final class MapRenderUtil {
    private MapRenderUtil() {}

    public static void fill(GuiGraphics graphics, double x1, double y1, double x2, double y2, int color) {
        double left = Math.min(x1, x2);
        double right = Math.max(x1, x2);
        double top = Math.min(y1, y2);
        double bottom = Math.max(y1, y2);

        if (right <= left) right = left + 0.5;
        if (bottom <= top) bottom = top + 0.5;

        quad(graphics, left, bottom, right, bottom, right, top, left, top, color);
    }

    public static void line(GuiGraphics graphics,
                            double x1, double y1,
                            double x2, double y2,
                            double thickness,
                            int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 0.0001) {
            double radius = thickness * 0.5;
            fill(graphics, x1 - radius, y1 - radius, x1 + radius, y1 + radius, color);
            return;
        }

        double half = thickness * 0.5;
        double nx = -dy / length * half;
        double ny = dx / length * half;
        quad(graphics, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, x1 + nx, y1 + ny, color);
    }

    private static void quad(GuiGraphics graphics,
                             double x1, double y1,
                             double x2, double y2,
                             double x3, double y3,
                             double x4, double y4,
                             int color) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;

        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();
        consumer.addVertex(matrix, (float) x1, (float) y1, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) x2, (float) y2, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) x3, (float) y3, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) x4, (float) y4, 0.0F).setColor(red, green, blue, alpha);
    }
}
