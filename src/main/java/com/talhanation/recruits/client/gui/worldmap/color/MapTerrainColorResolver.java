package com.talhanation.recruits.client.gui.worldmap.color;

import net.minecraft.client.multiplayer.ClientLevel;

public final class MapTerrainColorResolver {
    private MapTerrainColorResolver() {}

    public static int resolve(
            ClientLevel level,
            MapSample surface,
            MapSample underlay,
            int northHeight,
            int northWestHeight,
            int waterNeighbors,
            int waterRelief) {
        if (surface.isTransparentOverlay()) {
            return resolveTransparentOverlay(
                    level, surface, underlay, northHeight, northWestHeight, waterNeighbors, waterRelief);
        }

        if (surface.isWaterLike()) {
            return MapBlockColorResolver.resolveWaterNativeColor(
                    surface, underlay, waterNeighbors, waterRelief);
        }

        int rgb = surface.baseRgb();
        if ((rgb & 0x00FFFFFF) == 0) return 0x00000000;

        return MapBlockColorResolver.applyBrightnessToNativeColor(
                rgb, MapReliefShading.computeLandBrightness(level, surface, northHeight, northWestHeight));
    }

    public static int cachedHeight(MapSample sample, int fallback) {
        return sample != null && sample.isPresent() ? sample.height() : fallback;
    }

    private static int resolveTransparentOverlay(
            ClientLevel level,
            MapSample overlay,
            MapSample underlay,
            int northHeight,
            int northWestHeight,
            int waterNeighbors,
            int waterRelief) {
        int overlayRgb = overlay.baseRgb();
        if ((overlayRgb & 0x00FFFFFF) == 0) return 0x00000000;
        if (underlay == null || !underlay.isPresent()) {
            return MapBlockColorResolver.applyBrightnessToNativeColor(overlayRgb, 1.0F);
        }

        int baseNative =
                resolve(level, underlay, null, northHeight, northWestHeight, waterNeighbors, waterRelief);
        if (((baseNative >>> 24) & 0xFF) == 0) {
            return MapBlockColorResolver.applyBrightnessToNativeColor(overlayRgb, 1.0F);
        }

        int blendedRgb =
                MapBlockColorResolver.blendRgb(
                        nativeToRgb(baseNative),
                        overlayRgb,
                        MapStateClassifier.getTransparentOverlayAlpha(overlay.state()));
        return MapBlockColorResolver.applyBrightnessToNativeColor(blendedRgb, 1.0F);
    }

    private static int nativeToRgb(int nativeColor) {
        int red = nativeColor & 0xFF;
        int green = (nativeColor >>> 8) & 0xFF;
        int blue = (nativeColor >>> 16) & 0xFF;
        return red << 16 | green << 8 | blue;
    }
}
