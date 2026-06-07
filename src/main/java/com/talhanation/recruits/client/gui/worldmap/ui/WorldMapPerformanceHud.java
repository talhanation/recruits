package com.talhanation.recruits.client.gui.worldmap.ui;

import com.talhanation.recruits.client.gui.worldmap.debug.WorldMapDebugProfiler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public final class WorldMapPerformanceHud {
    private static final double SMOOTHING = 0.15;
    private static final long DEFAULT_FRAME_NANOS = 16_666_667L;

    private long lastFrameNanos;
    private double smoothedFrameNanos = DEFAULT_FRAME_NANOS;
    private double smoothedMapCostNanos;

    public void beginFrame() {
        long nowNanos = System.nanoTime();
        if (lastFrameNanos != 0L) {
            smoothFrameTiming(nowNanos - lastFrameNanos);
        }
        lastFrameNanos = nowNanos;
    }

    public void recordMapRender(long mapRenderWallNanos) {
        WorldMapDebugProfiler.HudStats stats = WorldMapDebugProfiler.hudStats();
        long measuredMapRenderNanos = Math.max(mapRenderWallNanos, stats.mapRenderNanos());
        smoothMapTiming(measuredMapRenderNanos + stats.tileUpdateNanos());
    }

    public void render(GuiGraphics guiGraphics, Font font, int screenWidth) {
        WorldMapDebugProfiler.HudStats stats = WorldMapDebugProfiler.hudStats();
        double fps = nanosToFps(smoothedFrameNanos);
        double mapMs = smoothedMapCostNanos / 1_000_000.0;
        double frameShare = smoothedFrameNanos <= 0.0
                ? 0.0
                : Math.min(999.0, smoothedMapCostNanos * 100.0 / smoothedFrameNanos);
        double mapFpsCost = estimatedMapFpsCost(fps);

        String line1 = String.format(Locale.ROOT, "FPS: %.0f", fps);
        String line2 = String.format(Locale.ROOT, "Map: %.1fms | %.0f%% | -%.0f fps", mapMs, frameShare, mapFpsCost);
        String line3 = String.format(Locale.ROOT, "Upd %.1fms C %.1f/S %.1f",
                stats.tileUpdateNanos() / 1_000_000.0,
                stats.consumeChunksNanos() / 1_000_000.0,
                stats.scheduleChunksNanos() / 1_000_000.0);
        String line4 = String.format(Locale.ROOT, "Q %d/%d | LOD %d/%d | L%d %d/%d miss %d",
                stats.chunkQueueSize(),
                stats.queuedChunkCount(),
                stats.lodTileCount(),
                stats.pendingLodTiles(),
                stats.rootLevel(),
                stats.tileDraws(),
                stats.visibleTiles(),
                stats.missingTiles());

        int textWidth = Math.max(
                Math.max(font.width(line1), font.width(line2)),
                Math.max(font.width(line3), font.width(line4)));
        int hudWidth = textWidth + 12;
        int hudHeight = 44;
        int hudX = Math.max(4, screenWidth - hudWidth - 8);
        int hudY = 8;
        guiGraphics.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, 0x90000000);
        guiGraphics.renderOutline(hudX, hudY, hudWidth, hudHeight, 0x50FFFFFF);
        guiGraphics.drawString(font, line1, hudX + 6, hudY + 5, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, line2, hudX + 6, hudY + 15, performanceColor(mapMs), false);
        guiGraphics.drawString(font, line3, hudX + 6, hudY + 25, 0xFFD8D8D8, false);
        guiGraphics.drawString(font, line4, hudX + 6, hudY + 35, 0xFFD8D8D8, false);
    }

    private void smoothFrameTiming(long frameNanos) {
        if (frameNanos <= 0L) return;
        smoothedFrameNanos += (frameNanos - smoothedFrameNanos) * SMOOTHING;
    }

    private void smoothMapTiming(long mapCostNanos) {
        if (mapCostNanos < 0L) return;
        if (smoothedMapCostNanos <= 0.0) {
            smoothedMapCostNanos = mapCostNanos;
        } else {
            smoothedMapCostNanos += (mapCostNanos - smoothedMapCostNanos) * SMOOTHING;
        }
    }

    private double estimatedMapFpsCost(double fps) {
        double withoutMapNanos = smoothedFrameNanos - smoothedMapCostNanos;
        if (withoutMapNanos <= 1_000_000.0) return 0.0;

        return Math.max(0.0, nanosToFps(withoutMapNanos) - fps);
    }

    private static double nanosToFps(double nanos) {
        return nanos <= 0.0 ? 0.0 : 1_000_000_000.0 / nanos;
    }

    private static int performanceColor(double mapMs) {
        if (mapMs >= 16.0) return 0xFFFF7070;
        if (mapMs >= 8.0) return 0xFFFFD060;
        return 0xFF80FF90;
    }
}
