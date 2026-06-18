package com.talhanation.recruits.client.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.talhanation.recruits.Main;
// client-only debug store (no networking)
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * In-world overlay for the recruit pathfinder.
 *
 * For the currently published debug snapshots it draws:
 *   - every VISITED node as a small wireframe box, coloured green->red by its
 *     f-cost relative to the most expensive visited node (so you can read the
 *     cost field at a glance),
 *   - the CHOSEN PATH nodes as larger boxes connected by a line,
 *   - the TARGET as a distinct box,
 *   - a floating cost label (f / malus) on each path node.
 *
 * Everything is gated on {@link ClientPathDebug#isEnabled()} client-side, which is
 * driven by the snapshots actually arriving from the server.
 *
 * Toggle with ClientPathDebug#setEnabled / #toggle (wired to debug id 27).
 * When off, the pathfinder publishes nothing and this renders nothing.
 */
@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public class PathDebugRenderer {

    private static final float BOX_VISITED = 0.18F;
    private static final float BOX_PATH = 0.32F;
    private static final float BOX_TARGET = 0.50F;
    private static final long STALE_MS = 5000L;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!ClientPathDebug.isEnabled()) return;

        var snapshots = ClientPathDebug.getSnapshots();
        if (snapshots.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        PoseStack pose = event.getPoseStack();
        long now = System.currentTimeMillis();

        // ---- boxes + lines (immediate mode) --------------------------------
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);

        for (ClientPathDebug.Snapshot snap : snapshots.values()) {
            if (now - snap.timestamp() > STALE_MS) continue;

            // max cost for normalising the heat colour
            float maxF = 1.0F;
            for (ClientPathDebug.Entry e : snap.visited()) maxF = Math.max(maxF, e.f());

            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            var matrix = pose.last().pose();

            // visited nodes (cost field)
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (ClientPathDebug.Entry e : snap.visited()) {
                float t = Math.min(1.0F, e.f() / maxF);
                // green (cheap) -> red (expensive)
                float r = t;
                float g = 1.0F - t;
                drawBox(buffer, matrix, e.x(), e.y(), e.z(), BOX_VISITED, r, g, 0.1F, 0.35F);
            }
            tesselator.end();

            // path nodes (brighter, bigger)
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (ClientPathDebug.Entry e : snap.pathNodes()) {
                drawBox(buffer, matrix, e.x(), e.y(), e.z(), BOX_PATH, 0.2F, 0.6F, 1.0F, 0.9F);
            }
            // target box
            if (snap.target() != null) {
                drawBox(buffer, matrix,
                        snap.target().getX(), snap.target().getY(), snap.target().getZ(),
                        BOX_TARGET,
                        snap.partial() ? 1.0F : 1.0F,
                        snap.partial() ? 0.4F : 1.0F,
                        0.0F, 1.0F);
            }
            tesselator.end();

            // connecting line along the path
            List<ClientPathDebug.Entry> path = snap.pathNodes();
            if (path.size() >= 2) {
                buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                for (ClientPathDebug.Entry e : path) {
                    buffer.vertex(matrix, e.x() + 0.5F, e.y() + 0.1F, e.z() + 0.5F)
                            .color(1.0F, 1.0F, 0.2F, 1.0F).endVertex();
                }
                tesselator.end();
            }

            pose.popPose();
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        // ---- cost labels (billboarded text) --------------------------------
        Font font = mc.font;
        MultiBufferSource.BufferSource textBuffers = mc.renderBuffers().bufferSource();
        for (ClientPathDebug.Snapshot snap : snapshots.values()) {
            if (now - snap.timestamp() > STALE_MS) continue;
            for (ClientPathDebug.Entry e : snap.pathNodes()) {
                String label = String.format("%.1f | m%.1f", e.f(), e.malus());
                drawLabel(pose, textBuffers, font, camera, cam, label,
                        e.x() + 0.5, e.y() + 0.6, e.z() + 0.5, 0xFFFFFF);
            }
            if (snap.target() != null) {
                drawLabel(pose, textBuffers, font, camera, cam,
                        snap.partial() ? "TARGET (partial)" : "TARGET",
                        snap.target().getX() + 0.5, snap.target().getY() + 1.1, snap.target().getZ() + 0.5,
                        0xFFAA00);
            }
        }
        textBuffers.endBatch();
    }

    private static void drawLabel(PoseStack pose, MultiBufferSource buffers, Font font,
                                  Camera camera, Vec3 cam, String text,
                                  double x, double y, double z, int color) {
        pose.pushPose();
        pose.translate(x - cam.x, y - cam.y, z - cam.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        var matrix = pose.last().pose();
        float w = -font.width(text) / 2.0F;
        font.drawInBatch(text, w, 0, color, false, matrix, buffers,
                Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        pose.popPose();
    }

    private static void drawBox(BufferBuilder buf, org.joml.Matrix4f m,
                                int bx, int by, int bz, float inset,
                                float r, float g, float b, float a) {
        float x0 = bx + inset, y0 = by + inset, z0 = bz + inset;
        float x1 = bx + 1 - inset, y1 = by + 1 - inset, z1 = bz + 1 - inset;

        // 12 edges as line pairs
        line(buf, m, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buf, m, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buf, m, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buf, m, x0, y0, z1, x0, y0, z0, r, g, b, a);

        line(buf, m, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buf, m, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buf, m, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buf, m, x0, y1, z1, x0, y1, z0, r, g, b, a);

        line(buf, m, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buf, m, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buf, m, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buf, m, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(BufferBuilder buf, org.joml.Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, z0).color(r, g, b, a).endVertex();
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).endVertex();
    }
}