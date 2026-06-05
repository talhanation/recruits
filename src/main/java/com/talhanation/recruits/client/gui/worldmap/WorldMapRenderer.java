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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WorldMapRenderer {
    static final boolean ENABLE_ZOOM_OUT_LOD = true;
    private static final MapFramebufferPass FRAMEBUFFER_PASS = new MapFramebufferPass();
    private static final int MAX_VISIBLE_TEXTURE_LEVEL = 2;
    private static final double THIRD_LOD_SCALE_THRESHOLD = 0.20;
    private static final int PREFETCH_TILE_MARGIN = 0;
    private static final int MAX_TILE_DRAWS_PER_FRAME = ENABLE_ZOOM_OUT_LOD ? 768 : 1536;
    private static final double TILE_SEAM_GUARD_PIXELS = 0.25;
    private static final double LOG_TWO = Math.log(2.0);

    private final WorldMapTileManager tileManager;
    private final WorldMapRenderTileCache renderTileCache;
    private final List<VisibleTile> visibleTiles = new ArrayList<>();
    private final List<VisibleTile> preparedTiles = new ArrayList<>();
    private final List<WorldMapRenderTileKey> preparedTileKeys = new ArrayList<>();
    private final Map<ResourceLocation, DrawBatch> drawBatches = new LinkedHashMap<>();
    private int visibleLevel = -1;
    private int visibleStartX = Integer.MIN_VALUE;
    private int visibleEndX = Integer.MIN_VALUE;
    private int visibleStartZ = Integer.MIN_VALUE;
    private int visibleEndZ = Integer.MIN_VALUE;
    private int visibleCenterX = Integer.MIN_VALUE;
    private int visibleCenterZ = Integer.MIN_VALUE;

    WorldMapRenderer(WorldMapTileManager tileManager) {
        this.tileManager = tileManager;
        this.renderTileCache = tileManager.getRenderTileCache();
    }

    void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, double offsetX, double offsetZ, double scale) {
        WorldMapDebugProfiler.beginMapRender(screenWidth, screenHeight, scale);

        long framebufferBeginStart = System.nanoTime();
        tileManager.beginRenderFrame();
        MapFramebufferPass.Frame frame = FRAMEBUFFER_PASS.begin(guiGraphics, offsetX, offsetZ, scale, screenWidth, screenHeight);
        WorldMapDebugProfiler.recordFramebufferBegin(System.nanoTime() - framebufferBeginStart);
        WorldMapDebugProfiler.recordFramebuffer(frame);

        long tileRenderStart = System.nanoTime();
        renderVisibleTiles(guiGraphics, frame, scale, getMapBrightness());
        WorldMapDebugProfiler.recordTileRender(System.nanoTime() - tileRenderStart);

        long framebufferBlitStart = System.nanoTime();
        FRAMEBUFFER_PASS.endAndBlit(guiGraphics, frame);
        WorldMapDebugProfiler.recordFramebufferBlit(System.nanoTime() - framebufferBlitStart);
        WorldMapDebugProfiler.finishMapRender();
    }

    void close() {
        visibleTiles.clear();
        preparedTiles.clear();
        preparedTileKeys.clear();
        drawBatches.clear();
    }

    static void releaseSharedResources() {
        FRAMEBUFFER_PASS.close();
    }

    static void prepareSharedResources() {
        FRAMEBUFFER_PASS.prepare();
    }

    private void renderVisibleTiles(GuiGraphics guiGraphics, MapFramebufferPass.Frame frame,
                                    double scale, float brightness) {
        int level = selectTextureLevel(scale);
        List<VisibleTile> visible = collectVisibleTiles(
                level,
                frame.leftWorld(),
                frame.rightWorld(),
                frame.topWorld(),
                frame.bottomWorld()
        );
        long regionPrefetchStartNanos = System.nanoTime();
        renderTileCache.prepareVisible(preparedTileKeys);
        WorldMapDebugProfiler.recordRegionPrefetch(System.nanoTime() - regionPrefetchStartNanos);
        WorldMapDebugProfiler.recordRootLevel(level, visible.size());

        RenderBudget budget = new RenderBudget(MAX_TILE_DRAWS_PER_FRAME);
        boolean measureDetails = WorldMapDebugProfiler.measureRenderDetails();
        clearDrawBatches();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            for (VisibleTile visibleTile : visible) {
                WorldMapDebugProfiler.recordTileVisit();
                if (!renderBestAvailable(guiGraphics, visibleTile.key(), frame, budget, measureDetails)) {
                    WorldMapDebugProfiler.recordMissingTile();
                }
            }
            drawBatches(guiGraphics, frame, measureDetails);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private boolean renderBestAvailable(GuiGraphics guiGraphics, WorldMapRenderTileKey requested,
                                        MapFramebufferPass.Frame frame, RenderBudget budget,
                                        boolean measureDetails) {
        WorldMapRenderTileCache.TileView best = renderTileCache.findBestAvailable(requested);
        if (best != null) {
            return renderTile(guiGraphics, requested, best, frame, budget, measureDetails);
        }
        return renderAvailableChildren(guiGraphics, requested, frame, budget, measureDetails);
    }

    private boolean renderAvailableChildren(GuiGraphics guiGraphics, WorldMapRenderTileKey parent,
                                            MapFramebufferPass.Frame frame, RenderBudget budget,
                                            boolean measureDetails) {
        if (parent.level() <= 0) return false;

        boolean rendered = false;
        for (int childZ = 0; childZ < 2; childZ++) {
            for (int childX = 0; childX < 2; childX++) {
                WorldMapRenderTileKey child = parent.child(childX, childZ);
                WorldMapRenderTileCache.TileView exact = renderTileCache.findExact(child);
                if (exact != null) {
                    rendered |= renderTile(guiGraphics, child, exact, frame, budget, measureDetails);
                } else {
                    rendered |= renderAvailableChildren(guiGraphics, child, frame, budget, measureDetails);
                }
            }
        }
        if (rendered) WorldMapDebugProfiler.recordChildSubstitution();
        return rendered;
    }

    private boolean renderTile(GuiGraphics guiGraphics, WorldMapRenderTileKey key,
                               WorldMapRenderTileCache.TileView tile, MapFramebufferPass.Frame frame,
                               RenderBudget budget, boolean measureDetails) {
        return queueTile(key, tile, budget);
    }

    private List<VisibleTile> collectVisibleTiles(int level, double leftWorld, double rightWorld,
                                                  double topWorld, double bottomWorld) {
        int tileSize = WorldMapRenderTileKey.PIXEL_SIZE << level;
        int startX = (int) Math.floor(leftWorld / tileSize);
        int endX = (int) Math.floor(rightWorld / tileSize);
        int startZ = (int) Math.floor(topWorld / tileSize);
        int endZ = (int) Math.floor(bottomWorld / tileSize);

        double centerWorldX = (leftWorld + rightWorld) * 0.5;
        double centerWorldZ = (topWorld + bottomWorld) * 0.5;
        int centerX = (int) Math.floor(centerWorldX / tileSize);
        int centerZ = (int) Math.floor(centerWorldZ / tileSize);

        if (level == visibleLevel
                && startX == visibleStartX && endX == visibleEndX
                && startZ == visibleStartZ && endZ == visibleEndZ
                && centerX == visibleCenterX && centerZ == visibleCenterZ) {
            return visibleTiles;
        }

        visibleLevel = level;
        visibleStartX = startX;
        visibleEndX = endX;
        visibleStartZ = startZ;
        visibleEndZ = endZ;
        visibleCenterX = centerX;
        visibleCenterZ = centerZ;
        visibleTiles.clear();
        preparedTiles.clear();
        preparedTileKeys.clear();
        for (int tileZ = startZ; tileZ <= endZ; tileZ++) {
            for (int tileX = startX; tileX <= endX; tileX++) {
                WorldMapRenderTileKey key = new WorldMapRenderTileKey(level, tileX, tileZ);
                visibleTiles.add(new VisibleTile(key, distanceToCenter(key, centerWorldX, centerWorldZ)));
            }
        }
        visibleTiles.sort(Comparator.comparingDouble(VisibleTile::centerDistance));
        for (int tileZ = startZ - PREFETCH_TILE_MARGIN; tileZ <= endZ + PREFETCH_TILE_MARGIN; tileZ++) {
            for (int tileX = startX - PREFETCH_TILE_MARGIN; tileX <= endX + PREFETCH_TILE_MARGIN; tileX++) {
                WorldMapRenderTileKey key = new WorldMapRenderTileKey(level, tileX, tileZ);
                preparedTiles.add(new VisibleTile(key, distanceToCenter(key, centerWorldX, centerWorldZ)));
            }
        }
        preparedTiles.sort(Comparator.comparingDouble(VisibleTile::centerDistance));
        for (VisibleTile preparedTile : preparedTiles) {
            preparedTileKeys.add(preparedTile.key());
        }
        return visibleTiles;
    }

    private static int selectTextureLevel(double scale) {
        if (!ENABLE_ZOOM_OUT_LOD || scale >= 1.0) return 0;
        if (scale <= THIRD_LOD_SCALE_THRESHOLD) return MAX_VISIBLE_TEXTURE_LEVEL;
        double reversedScale = 1.0 / Math.max(0.01, scale);
        return Mth.clamp((int) Math.floor(Math.log(reversedScale) / LOG_TWO), 0, MAX_VISIBLE_TEXTURE_LEVEL - 1);
    }

    private static double distanceToCenter(WorldMapRenderTileKey key, double centerWorldX, double centerWorldZ) {
        double tileCenterWorldX = key.worldMinX() + key.worldSize() * 0.5;
        double tileCenterWorldZ = key.worldMinZ() + key.worldSize() * 0.5;
        double dx = tileCenterWorldX - centerWorldX;
        double dz = tileCenterWorldZ - centerWorldZ;
        return dx * dx + dz * dz;
    }

    private boolean queueTile(WorldMapRenderTileKey key, WorldMapRenderTileCache.TileView tile,
                              RenderBudget budget) {
        if (!budget.tryDraw()) return false;
        drawBatches.computeIfAbsent(tile.textureId(), DrawBatch::new).tiles.add(new DrawTile(key, tile));
        return true;
    }

    private void clearDrawBatches() {
        for (DrawBatch drawBatch : drawBatches.values()) {
            drawBatch.tiles.clear();
        }
    }

    private void drawBatches(GuiGraphics guiGraphics, MapFramebufferPass.Frame frame, boolean measureDetails) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        Iterator<DrawBatch> iterator = drawBatches.values().iterator();
        while (iterator.hasNext()) {
            DrawBatch drawBatch = iterator.next();
            if (drawBatch.tiles.isEmpty()) {
                iterator.remove();
                continue;
            }

            long startNanos = measureDetails ? System.nanoTime() : 0L;
            RenderSystem.setShaderTexture(0, drawBatch.textureId);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (DrawTile drawTile : drawBatch.tiles) {
                appendWorldTile(buffer, matrix, drawTile, frame);
            }
            BufferUploader.drawWithShader(buffer.end());
            if (measureDetails) {
                WorldMapDebugProfiler.recordWorldTileDraw(System.nanoTime() - startNanos);
            }
        }
    }

    private static void appendWorldTile(BufferBuilder buffer, Matrix4f matrix, DrawTile drawTile,
                                        MapFramebufferPass.Frame frame) {
        WorldMapRenderTileKey key = drawTile.key;
        WorldMapRenderTileCache.TileView tile = drawTile.tile;
        double screenX = frame.renderOffsetX() + key.worldMinX() * frame.fboScale();
        double screenZ = frame.renderOffsetZ() + key.worldMinZ() * frame.fboScale();
        double screenSize = key.worldSize() * frame.fboScale();
        if (screenSize < 0.75) return;

        double seamGuard = Math.min(TILE_SEAM_GUARD_PIXELS, screenSize * 0.01);
        double x1 = screenX - seamGuard;
        double z1 = screenZ - seamGuard;
        double x2 = screenX + screenSize + seamGuard;
        double z2 = screenZ + screenSize + seamGuard;

        buffer.vertex(matrix, (float) x1, (float) z2, 0.0F).uv(tile.u1(), tile.v2()).endVertex();
        buffer.vertex(matrix, (float) x2, (float) z2, 0.0F).uv(tile.u2(), tile.v2()).endVertex();
        buffer.vertex(matrix, (float) x2, (float) z1, 0.0F).uv(tile.u2(), tile.v1()).endVertex();
        buffer.vertex(matrix, (float) x1, (float) z1, 0.0F).uv(tile.u1(), tile.v1()).endVertex();
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

    private record VisibleTile(WorldMapRenderTileKey key, double centerDistance) {
    }

    private record DrawTile(WorldMapRenderTileKey key, WorldMapRenderTileCache.TileView tile) {
    }

    private static final class DrawBatch {
        private final ResourceLocation textureId;
        private final List<DrawTile> tiles = new ArrayList<>();

        private DrawBatch(ResourceLocation textureId) {
            this.textureId = textureId;
        }
    }

    private static final class RenderBudget {
        private int drawsLeft;

        private RenderBudget(int drawsLeft) {
            this.drawsLeft = drawsLeft;
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
    }
}
