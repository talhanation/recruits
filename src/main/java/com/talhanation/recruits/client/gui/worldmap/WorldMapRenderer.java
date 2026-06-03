package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class WorldMapRenderer {
    private static final int MAX_LOD_LEVEL = 2;
    private static final int MAX_SAMPLE_STEP = 1 << MAX_LOD_LEVEL;
    private static final int MAX_TILE_DRAWS_PER_FRAME = 384;
    private static final int MAX_TILE_VISITS_PER_FRAME = 4096;
    private static final int MAX_TEXTURE_UPLOADS_PER_FRAME = 2;
    private static final double CHILD_SUBSTITUTION_MIN_PIXELS = 256.0;
    private static final double MISSING_TILE_FALLBACK_MIN_PIXELS = 24.0;
    private static final int MAX_MISSING_TILE_FALLBACK_DEPTH = 4;
    private static final double TILE_SEAM_GUARD_PIXELS = 0.25;

    private final WorldMapTileManager tileManager;
    private final MapFramebufferPass framebufferPass = new MapFramebufferPass();

    WorldMapRenderer(WorldMapTileManager tileManager) {
        this.tileManager = tileManager;
    }

    void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, double offsetX, double offsetZ, double scale) {
        WorldMapDebugProfiler.beginMapRender(screenWidth, screenHeight, scale);

        long framebufferBeginStart = System.nanoTime();
        tileManager.beginRenderFrame();
        MapFramebufferPass.Frame frame = framebufferPass.begin(guiGraphics, offsetX, offsetZ, scale, screenWidth, screenHeight);
        WorldMapDebugProfiler.recordFramebufferBegin(System.nanoTime() - framebufferBeginStart);
        WorldMapDebugProfiler.recordFramebuffer(frame);

        long tileRenderStart = System.nanoTime();
        renderTileTree(guiGraphics, frame, getMapBrightness());
        WorldMapDebugProfiler.recordTileRender(System.nanoTime() - tileRenderStart);

        long framebufferBlitStart = System.nanoTime();
        framebufferPass.endAndBlit(guiGraphics, frame);
        WorldMapDebugProfiler.recordFramebufferBlit(System.nanoTime() - framebufferBlitStart);
        WorldMapDebugProfiler.finishMapRender();
    }

    void close() {
        framebufferPass.close();
    }

    private void renderTileTree(GuiGraphics guiGraphics, MapFramebufferPass.Frame frame, float brightness) {
        int rootLevel = chooseRootLevel(frame.fboScale());
        int rootWorldSize = worldSizeForLevel(rootLevel);
        List<TileCoord> visibleTiles = collectVisibleTiles(
                frame.leftWorld(),
                frame.rightWorld(),
                frame.topWorld(),
                frame.bottomWorld(),
                rootWorldSize
        );

        WorldMapLodCache lodCache = tileManager.getLodCache();
        lodCache.beginFrame();
        if (rootLevel <= 1) {
            tileManager.scheduleVisibleRegionLoads(frame.leftWorld(), frame.rightWorld(), frame.topWorld(), frame.bottomWorld());
        }
        WorldMapDebugProfiler.recordRootLevel(rootLevel, visibleTiles.size());
        RenderBudget budget = new RenderBudget(
                MAX_TILE_DRAWS_PER_FRAME,
                MAX_TILE_VISITS_PER_FRAME,
                MAX_TEXTURE_UPLOADS_PER_FRAME
        );

        for (TileCoord tileCoord : visibleTiles) {
            renderTileTree(guiGraphics, lodCache, budget, rootLevel, tileCoord.x(), tileCoord.z(),
                    frame.renderOffsetX(), frame.renderOffsetZ(), frame.fboScale(), brightness, 0, true);
        }

        lodCache.trim();
    }

    private boolean renderTileTree(GuiGraphics guiGraphics, WorldMapLodCache lodCache, RenderBudget budget,
                                   int level, int tileX, int tileZ,
                                   double offsetX, double offsetZ, double scale,
                                   float brightness,
                                   int missingFallbackDepth, boolean allowScheduling) {
        if (!budget.tryVisit()) return false;

        int worldSize = worldSizeForLevel(level);
        double screenSize = worldSize * scale;
        if (screenSize < 0.75) return false;

        TileTexture texture = acquireTileTexture(lodCache, level, tileX, tileZ, allowScheduling, budget);
        if (texture != null) {
            if (shouldSubstituteWithChildren(level, screenSize) && canRenderImmediateChildren(lodCache, level, tileX, tileZ)) {
                WorldMapDebugProfiler.recordChildSubstitution();
                return renderImmediateChildren(guiGraphics, lodCache, budget, level, tileX, tileZ, offsetX, offsetZ, scale, brightness);
            }
            if (shouldSubstituteWithChildren(level, screenSize) && allowScheduling) {
                scheduleImmediateChildren(lodCache, level, tileX, tileZ);
            }
            if (!budget.tryDraw()) return false;
            renderWorldTile(guiGraphics, texture.textureId(), texture.worldMinX(), texture.worldMinZ(),
                    texture.worldSize(), offsetX, offsetZ, scale, brightness);
            return true;
        }

        WorldMapDebugProfiler.recordMissingTile();

        boolean shouldFallbackToChildren = level > 0
                && allowScheduling
                && missingFallbackDepth < MAX_MISSING_TILE_FALLBACK_DEPTH
                && screenSize >= MISSING_TILE_FALLBACK_MIN_PIXELS;
        if (!shouldFallbackToChildren) return false;

        WorldMapDebugProfiler.recordMissingFallback();
        return renderChildren(guiGraphics, lodCache, budget, level, tileX, tileZ, offsetX, offsetZ, scale,
                brightness, missingFallbackDepth + 1, false);
    }

    private boolean renderImmediateChildren(GuiGraphics guiGraphics, WorldMapLodCache lodCache, RenderBudget budget,
                                            int level, int tileX, int tileZ,
                                            double offsetX, double offsetZ, double scale, float brightness) {
        if (!budget.hasDrawsLeft(4)) return false;

        int childLevel = level - 1;
        int childBaseX = tileX * 2;
        int childBaseZ = tileZ * 2;
        for (int childZ = 0; childZ < 2; childZ++) {
            for (int childX = 0; childX < 2; childX++) {
                TileTexture childTexture = acquireTileTexture(lodCache, childLevel, childBaseX + childX, childBaseZ + childZ, false, budget);
                if (childTexture == null || !budget.tryDraw()) return false;
                renderWorldTile(guiGraphics, childTexture.textureId(), childTexture.worldMinX(), childTexture.worldMinZ(),
                        childTexture.worldSize(), offsetX, offsetZ, scale, brightness);
            }
        }
        return true;
    }

    private boolean renderChildren(GuiGraphics guiGraphics, WorldMapLodCache lodCache, RenderBudget budget,
                                   int level, int tileX, int tileZ,
                                   double offsetX, double offsetZ, double scale,
                                   float brightness,
                                   int missingFallbackDepth, boolean allowScheduling) {
        int childLevel = level - 1;
        int childBaseX = tileX * 2;
        int childBaseZ = tileZ * 2;
        boolean renderedChild = false;
        for (int childZ = 0; childZ < 2; childZ++) {
            for (int childX = 0; childX < 2; childX++) {
                renderedChild |= renderTileTree(guiGraphics, lodCache, budget, childLevel,
                        childBaseX + childX, childBaseZ + childZ,
                        offsetX, offsetZ, scale, brightness, missingFallbackDepth, allowScheduling);
            }
        }
        return renderedChild;
    }

    private boolean canRenderImmediateChildren(WorldMapLodCache lodCache, int level, int tileX, int tileZ) {
        int childLevel = level - 1;
        int childBaseX = tileX * 2;
        int childBaseZ = tileZ * 2;
        for (int childZ = 0; childZ < 2; childZ++) {
            for (int childX = 0; childX < 2; childX++) {
                if (acquireTileTexture(lodCache, childLevel, childBaseX + childX, childBaseZ + childZ, false, null) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void scheduleImmediateChildren(WorldMapLodCache lodCache, int level, int tileX, int tileZ) {
        int childLevel = level - 1;
        int childBaseX = tileX * 2;
        int childBaseZ = tileZ * 2;
        for (int childZ = 0; childZ < 2; childZ++) {
            for (int childX = 0; childX < 2; childX++) {
                acquireTileTexture(lodCache, childLevel, childBaseX + childX, childBaseZ + childZ, true, null);
            }
        }
    }

    private TileTexture acquireTileTexture(WorldMapLodCache lodCache, int level, int tileX, int tileZ,
                                           boolean allowScheduling, RenderBudget budget) {
        ResourceLocation textureId;
        int worldSize = worldSizeForLevel(level);
        int worldMinX = tileX * worldSize;
        int worldMinZ = tileZ * worldSize;

        if (level == 0) {
            WorldMapRegionTile region = tileManager.getLoadedRegion(tileX, tileZ);
            if (region == null && allowScheduling) {
                region = tileManager.getOrScheduleCachedRegion(tileX, tileZ);
            }
            if (region == null) return null;
            textureId = region.getTextureId(region.needsTextureUpload() && tryTextureUpload(budget));
        } else {
            WorldMapLodTile lodTile = allowScheduling
                    ? lodCache.getOrSchedule(tileX, tileZ, sampleStepForLevel(level))
                    : lodCache.getIfPresent(tileX, tileZ, sampleStepForLevel(level));
            if (lodTile == null) return null;
            textureId = lodTile.getTextureId(lodTile.hasUploadReady() && tryTextureUpload(budget));
        }

        if (textureId == null) return null;
        return new TileTexture(textureId, worldMinX, worldMinZ, worldSize);
    }

    private static boolean tryTextureUpload(RenderBudget budget) {
        return budget != null && budget.tryUpload();
    }

    private static int chooseRootLevel(double scale) {
        if (scale >= 1.0) return 0;

        double reversedScale = 1.0 / Math.max(scale, 0.01);
        int level = (int) Math.floor(Math.log(reversedScale) / Math.log(2.0));
        return Math.max(0, Math.min(level, MAX_LOD_LEVEL));
    }

    private static boolean shouldSubstituteWithChildren(int level, double screenSize) {
        return level > 0 && screenSize >= CHILD_SUBSTITUTION_MIN_PIXELS;
    }

    private static int worldSizeForLevel(int level) {
        return WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStepForLevel(level);
    }

    private static int sampleStepForLevel(int level) {
        int clampedLevel = Math.max(0, Math.min(level, MAX_LOD_LEVEL));
        return Math.min(MAX_SAMPLE_STEP, 1 << clampedLevel);
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
                                        double offsetX, double offsetZ, double scale, float brightness) {
        double screenX = offsetX + worldMinX * scale;
        double screenZ = offsetZ + worldMinZ * scale;
        double screenSize = worldSize * scale;
        if (screenSize < 0.75) return;

        double x1 = screenX;
        double z1 = screenZ;
        double x2 = screenX + screenSize;
        double z2 = screenZ + screenSize;
        double seamGuard = Math.min(TILE_SEAM_GUARD_PIXELS, screenSize * 0.01);
        x1 -= seamGuard;
        z1 -= seamGuard;
        x2 += seamGuard;
        z2 += seamGuard;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
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
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float getMapBrightness() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimensionType() == null || !level.dimensionType().hasSkyLight()) {
            return 1.0F;
        }

        float ambient = Math.min(1.0F, 0.375F + level.dimensionType().ambientLight());
        float sunBrightness = (level.getSkyDarken(1.0F) - 0.2F) / 0.8F;
        return ambient + (1.0F - ambient) * Mth.clamp(sunBrightness, 0.0F, 1.0F);
    }

    private record TileCoord(int x, int z, double centerDistance) {
    }

    private record TileTexture(ResourceLocation textureId, int worldMinX, int worldMinZ, int worldSize) {
    }

    private static final class RenderBudget {
        private int drawsLeft;
        private int visitsLeft;
        private int uploadsLeft;

        private RenderBudget(int drawsLeft, int visitsLeft, int uploadsLeft) {
            this.drawsLeft = drawsLeft;
            this.visitsLeft = visitsLeft;
            this.uploadsLeft = uploadsLeft;
        }

        private boolean tryVisit() {
            if (visitsLeft <= 0) {
                WorldMapDebugProfiler.recordVisitBudgetExhausted();
                return false;
            }
            visitsLeft--;
            WorldMapDebugProfiler.recordTileVisit();
            return true;
        }

        private boolean tryDraw() {
            if (drawsLeft <= 0) {
                WorldMapDebugProfiler.recordDrawBudgetExhausted();
                return false;
            }
            drawsLeft--;
            WorldMapDebugProfiler.recordTileDraw();
            return true;
        }

        private boolean hasDrawsLeft(int requiredDraws) {
            return drawsLeft >= requiredDraws;
        }

        private boolean tryUpload() {
            if (uploadsLeft <= 0) {
                WorldMapDebugProfiler.recordUploadBudgetExhausted();
                return false;
            }
            uploadsLeft--;
            return true;
        }
    }
}
