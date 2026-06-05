package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

final class MapReliefShading {
    private static final float AMBIENT_COLORED = 0.2f;
    private static final float AMBIENT_WHITE = 0.5f;
    private static final float MAX_DIRECT_LIGHT = 0.6666667f;

    private MapReliefShading() {
    }

    static float computeWaterBrightness(int shoreNeighbors, int depth, int relief) {
        float brightness = 0.94f - Math.min(depth, 12) * 0.018f;
        if (shoreNeighbors < 4) brightness += 0.05f;
        brightness += Math.min(0.02f, relief * 0.0025f);
        return clamp(brightness, 0.74f, 1.0f);
    }

    static ColorMultiplier computeLandBrightness(ClientLevel level, MapSample sample, int northHeight, int northWestHeight) {
        int height = sample.height();
        int verticalSlope = clampInt(height - northHeight, -128, 127);
        int diagonalSlope = clampInt(height - northWestHeight, -128, 127);
        float depthBrightness = clamp(height / 63.0f, 0.90f, 1.0f);
        float whiteLight = AMBIENT_WHITE + computeSlopeLight(verticalSlope, diagonalSlope);

        return new ColorMultiplier(
                (getShadowR(level) * AMBIENT_COLORED + whiteLight) * depthBrightness,
                (getShadowG(level) * AMBIENT_COLORED + whiteLight) * depthBrightness,
                (getShadowB(level) * AMBIENT_COLORED + whiteLight) * depthBrightness
        );
    }

    private static float computeSlopeLight(int verticalSlope, int diagonalSlope) {
        float cos = 0.0f;
        float crossZ = -verticalSlope;
        if (crossZ < 1.0f) {
            if (verticalSlope == 1 && diagonalSlope == 1) {
                cos = 1.0f;
            } else {
                float crossX = verticalSlope - diagonalSlope;
                float cast = 1.0f - crossZ;
                float crossMagnitude = (float) Math.sqrt(crossX * crossX + 1.0f + crossZ * crossZ);
                cos = (float) ((cast / crossMagnitude) / Math.sqrt(2.0));
            }
        }

        if (cos == 1.0f) return MAX_DIRECT_LIGHT;
        if (cos > 0.0f) {
            return (float) Math.ceil(cos * 10.0f) / 10.0f * MAX_DIRECT_LIGHT * 0.88388f;
        }

        return 0.0f;
    }

    private static float getShadowR(ClientLevel level) {
        if (level.dimension() == Level.OVERWORLD) return 0.518f;
        if (level.dimension() == Level.NETHER) return 1.0f;
        return 1.0f;
    }

    private static float getShadowG(ClientLevel level) {
        if (level.dimension() == Level.OVERWORLD) return 0.678f;
        if (level.dimension() == Level.NETHER) return 0.0f;
        return 1.0f;
    }

    private static float getShadowB(ClientLevel level) {
        if (level.dimension() == Level.OVERWORLD) return 1.0f;
        if (level.dimension() == Level.NETHER) return 0.0f;
        return 1.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
