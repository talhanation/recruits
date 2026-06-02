package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

final class MapReliefShading {
    private static final float AMBIENT_COLORED = 0.2f;
    private static final float AMBIENT_WHITE = 0.5f;
    private static final float MAX_DIRECT_LIGHT = 0.6666667f;

    private MapReliefShading() {
    }

    static float computeWaterBrightness(ClientLevel level, BlockPos pos) {
        int depth = MapStateSampler.getWaterDepth(level, pos);
        int shoreNeighbors = MapStateSampler.countWaterNeighbors(level, pos);
        int x = pos.getX();
        int z = pos.getZ();
        int heightNorth = MapStateSampler.getSurfaceHeight(level, x, z - 1);
        int heightSouth = MapStateSampler.getSurfaceHeight(level, x, z + 1);
        int heightWest = MapStateSampler.getSurfaceHeight(level, x - 1, z);
        int heightEast = MapStateSampler.getSurfaceHeight(level, x + 1, z);
        int relief = Math.abs(heightWest - heightEast) + Math.abs(heightNorth - heightSouth);

        float brightness = 0.95f - Math.min(depth, 12) * 0.016f;
        if (shoreNeighbors < 4) brightness += 0.05f;
        brightness += Math.min(0.025f, relief * 0.0025f);
        return clamp(brightness, 0.76f, 1.0f);
    }

    static ColorMultiplier computeLandBrightness(ClientLevel level, MapSample sample, int northHeight, int northWestHeight) {
        int height = sample.height();
        int verticalSlope = clampInt(height - northHeight, -128, 127);
        int diagonalSlope = clampInt(height - northWestHeight, -128, 127);
        float depthBrightness = clamp(height / 80.0f, 0.88f, 1.03f);
        float contour = contourBrightness(height, verticalSlope, diagonalSlope);
        float whiteLight = AMBIENT_WHITE + computeSlopeLight(verticalSlope, diagonalSlope);

        return new ColorMultiplier(
                (getShadowR(level) * AMBIENT_COLORED + whiteLight) * depthBrightness * contour,
                (getShadowG(level) * AMBIENT_COLORED + whiteLight) * depthBrightness * contour,
                (getShadowB(level) * AMBIENT_COLORED + whiteLight) * depthBrightness * contour
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

    private static float contourBrightness(int height, int verticalSlope, int diagonalSlope) {
        if (height <= 62 || (verticalSlope == 0 && diagonalSlope == 0)) return 1.0f;
        return Math.floorMod(height, 8) == 0 ? 0.94f : 1.0f;
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
