package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class WorldMapRenderer {
    private static final double BASE_REGION_SCALE_THRESHOLD = 0.55;
    private static final int MAX_SAMPLE_STEP = 16;
    private static final int FALLBACK_REGION_DRAWS_PER_FRAME = 128;

    private final WorldMapTileManager tileManager;
    private final MapFramebufferPass framebufferPass = new MapFramebufferPass();

    WorldMapRenderer(WorldMapTileManager tileManager) {
        this.tileManager = tileManager;
    }

    void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, double offsetX, double offsetZ, double scale) {
        tileManager.beginRenderFrame();

        MapFramebufferPass.Frame frame = framebufferPass.begin(guiGraphics, offsetX, offsetZ, scale, screenWidth, screenHeight);
        int sampleStep = chooseSampleStep(frame.fboScale());
        if (sampleStep <= 1) {
            renderRegionTiles(guiGraphics, frame);
        } else {
            renderLodTiles(guiGraphics, frame, sampleStep);
        }
        framebufferPass.endAndBlit(guiGraphics, frame);
    }

    void close() {
        framebufferPass.close();
    }

    private void renderRegionTiles(GuiGraphics guiGraphics, MapFramebufferPass.Frame frame) {
        List<TileCoord> visibleTiles = collectVisibleTiles(
                frame.leftWorld(),
                frame.rightWorld(),
                frame.topWorld(),
                frame.bottomWorld(),
                WorldMapRegionTile.REGION_PIXEL_SIZE
        );

        for (TileCoord tileCoord : visibleTiles) {
            WorldMapRegionTile region = tileManager.getLoadedRegion(tileCoord.x(), tileCoord.z());
            if (region == null) region = tileManager.getOrScheduleCachedRegion(tileCoord.x(), tileCoord.z());
            if (region == null) continue;

            ResourceLocation textureId = region.getTextureId();
            if (textureId == null) continue;
            renderWorldTile(guiGraphics, textureId,
                    tileCoord.x() * WorldMapRegionTile.REGION_PIXEL_SIZE,
                    tileCoord.z() * WorldMapRegionTile.REGION_PIXEL_SIZE,
                    WorldMapRegionTile.REGION_PIXEL_SIZE,
                    frame.renderOffsetX(), frame.renderOffsetZ(), frame.fboScale());
        }
    }

    private void renderLodTiles(GuiGraphics guiGraphics, MapFramebufferPass.Frame frame, int sampleStep) {
        int tileWorldSize = WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStep;
        List<TileCoord> visibleTiles = collectVisibleTiles(frame.leftWorld(), frame.rightWorld(),
                frame.topWorld(), frame.bottomWorld(), tileWorldSize);

        WorldMapLodCache lodCache = tileManager.getLodCache();
        lodCache.beginFrame();

        int fallbackDrawsLeft = FALLBACK_REGION_DRAWS_PER_FRAME;
        for (TileCoord tileCoord : visibleTiles) {
            WorldMapLodTile lodTile = lodCache.getOrSchedule(tileCoord.x(), tileCoord.z(), sampleStep);
            ResourceLocation textureId = lodTile.getTextureId();
            if (textureId != null) {
                renderWorldTile(guiGraphics, textureId, lodTile.getWorldMinX(), lodTile.getWorldMinZ(), tileWorldSize,
                        frame.renderOffsetX(), frame.renderOffsetZ(), frame.fboScale());
                continue;
            }

            if (fallbackDrawsLeft > 0) {
                fallbackDrawsLeft -= renderLoadedRegionsInArea(guiGraphics, lodTile.getWorldMinX(), lodTile.getWorldMinZ(),
                        lodTile.getWorldMaxX(), lodTile.getWorldMaxZ(), frame.renderOffsetX(), frame.renderOffsetZ(),
                        frame.fboScale(), fallbackDrawsLeft);
            }
        }

        lodCache.trim();
    }

    private int renderLoadedRegionsInArea(GuiGraphics guiGraphics, int minWorldX, int minWorldZ,
                                          int maxWorldX, int maxWorldZ, double offsetX, double offsetZ,
                                          double scale, int maxDraws) {
        int startRegionX = Math.floorDiv(minWorldX, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionX = Math.floorDiv(maxWorldX - 1, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int startRegionZ = Math.floorDiv(minWorldZ, WorldMapRegionTile.REGION_PIXEL_SIZE);
        int endRegionZ = Math.floorDiv(maxWorldZ - 1, WorldMapRegionTile.REGION_PIXEL_SIZE);

        int draws = 0;
        for (int regionZ = startRegionZ; regionZ <= endRegionZ; regionZ++) {
            for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
                if (draws >= maxDraws) return draws;
                WorldMapRegionTile region = tileManager.getLoadedRegion(regionX, regionZ);
                if (region == null) continue;

                ResourceLocation textureId = region.getTextureId();
                if (textureId == null) continue;
                renderWorldTile(guiGraphics, textureId,
                        regionX * WorldMapRegionTile.REGION_PIXEL_SIZE,
                        regionZ * WorldMapRegionTile.REGION_PIXEL_SIZE,
                        WorldMapRegionTile.REGION_PIXEL_SIZE,
                        offsetX, offsetZ, scale);
                draws++;
            }
        }
        return draws;
    }

    private static int chooseSampleStep(double scale) {
        if (scale >= BASE_REGION_SCALE_THRESHOLD) return 1;

        double target = 1.0 / Math.max(scale, 0.05);
        int lower = 1;
        while (lower * 2 <= target && lower * 2 <= MAX_SAMPLE_STEP) {
            lower *= 2;
        }

        int upper = Math.min(MAX_SAMPLE_STEP, lower * 2);
        int sampleStep = (target / lower <= upper / target) ? lower : upper;
        return Math.max(2, sampleStep);
    }

    private static List<TileCoord> collectVisibleTiles(double leftWorld, double rightWorld,
                                                       double topWorld, double bottomWorld,
                                                       int tileWorldSize) {
        int startTileX = (int) Math.floor(leftWorld / tileWorldSize) - 1;
        int endTileX = (int) Math.ceil(rightWorld / tileWorldSize) + 1;
        int startTileZ = (int) Math.floor(topWorld / tileWorldSize) - 1;
        int endTileZ = (int) Math.ceil(bottomWorld / tileWorldSize) + 1;

        double centerWorldX = (leftWorld + rightWorld) * 0.5;
        double centerWorldZ = (topWorld + bottomWorld) * 0.5;

        List<TileCoord> visibleTiles = new ArrayList<>();
        for (int tileZ = startTileZ; tileZ <= endTileZ; tileZ++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                visibleTiles.add(new TileCoord(tileX, tileZ, distanceToCenter(tileX, tileZ, tileWorldSize,
                        centerWorldX, centerWorldZ)));
            }
        }
        visibleTiles.sort(Comparator.comparingDouble(TileCoord::centerDistance));
        return visibleTiles;
    }

    private static double distanceToCenter(int tileX, int tileZ, int tileWorldSize,
                                           double centerWorldX, double centerWorldZ) {
        double tileCenterWorldX = (tileX + 0.5) * tileWorldSize;
        double tileCenterWorldZ = (tileZ + 0.5) * tileWorldSize;
        double dx = tileCenterWorldX - centerWorldX;
        double dz = tileCenterWorldZ - centerWorldZ;
        return dx * dx + dz * dz;
    }

    private static void renderWorldTile(GuiGraphics guiGraphics, ResourceLocation textureId,
                                        int worldMinX, int worldMinZ, int worldSize,
                                        double offsetX, double offsetZ, double scale) {
        double screenX = offsetX + worldMinX * scale;
        double screenZ = offsetZ + worldMinZ * scale;
        double screenSize = worldSize * scale;
        if (screenSize < 0.75) return;

        double x1 = screenX;
        double z1 = screenZ;
        double x2 = screenX + screenSize;
        double z2 = screenZ + screenSize;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, (float) x1, (float) z2, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, (float) x2, (float) z2, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, (float) x2, (float) z1, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, (float) x1, (float) z1, 0.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private record TileCoord(int x, int z, double centerDistance) {
    }
}
