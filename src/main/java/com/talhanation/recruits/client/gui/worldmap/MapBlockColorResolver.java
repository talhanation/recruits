package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MapBlockColorResolver {
    private static final int WATER_BLUE = 0x1F5DA8;
    private static final int MAX_TEXTURE_COLOR_CACHE_SIZE = 4096;
    private static final MapTintSampler TINT_SAMPLER = new MapTintSampler();
    private static final Map<BlockState, TextureColor> TEXTURE_COLOR_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<BlockState, TextureColor> eldest) {
                    return size() > MAX_TEXTURE_COLOR_CACHE_SIZE;
                }
            }
    );

    private MapBlockColorResolver() {
    }

    static int resolveBaseRgb(ClientLevel level, BlockPos pos, BlockState state) {
        MapColor mapColor = state.getMapColor(level, pos);
        int mapRgb = mapColor != null ? mapColor.col : 0;

        TextureColor textureColor = getTextureColor(state);
        int base = textureColor.rgb() != 0 ? textureColor.rgb() : mapRgb;
        if ((base & 0x00FFFFFF) == 0) return 0;

        int tint = resolveBiomeTint(level, pos, state, textureColor.tintIndex());
        if (tint != -1 && (tint & 0x00FFFFFF) != 0) {
            base = multiplyBiomeTint(base, tint);
        }

        return tuneTerrainRgb(base);
    }

    static int resolveWaterRgb(ClientLevel level, BlockPos pos, BlockState state) {
        int waterRgb = blendRgb(resolveBaseRgb(level, pos, state), WATER_BLUE, 0.32f);
        MapSample floor = MapStateSampler.findUnderWaterSample(level, pos);
        if (floor == null) return waterRgb;

        int floorRgb = resolveBaseRgb(level, floor.pos(), floor.state());
        if ((floorRgb & 0x00FFFFFF) == 0) return waterRgb;

        int depth = Math.max(1, pos.getY() - floor.height());
        float waterAlpha = clamp(0.48f + Math.min(depth, 7) * 0.065f, 0.52f, 0.84f);
        return blendRgb(floorRgb, waterRgb, waterAlpha);
    }

    static int applyBrightnessToNativeColor(int rgb, float brightness) {
        return applyBrightnessToNativeColor(rgb, ColorMultiplier.uniform(brightness));
    }

    static int applyBrightnessToNativeColor(int rgb, ColorMultiplier brightness) {
        int red = clampColor(Math.round(((rgb >> 16) & 0xFF) * brightness.red()));
        int green = clampColor(Math.round(((rgb >> 8) & 0xFF) * brightness.green()));
        int blue = clampColor(Math.round((rgb & 0xFF) * brightness.blue()));

        return 0xFF000000 | (blue << 16) | (green << 8) | red;
    }

    private static int resolveBiomeTint(ClientLevel level, BlockPos pos, BlockState state, int tintIndex) {
        if (tintIndex < 0) return -1;

        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        return blockColors.getColor(state, TINT_SAMPLER.use(level), pos, tintIndex);
    }

    private static TextureColor getTextureColor(BlockState state) {
        TextureColor cached = TEXTURE_COLOR_CACHE.get(state);
        if (cached != null) return cached;

        TextureColor result;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BakedModel model = minecraft.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
            List<BakedQuad> upQuads = model.getQuads(state, Direction.UP, RandomSource.create(42L), ModelData.EMPTY, null);
            result = !upQuads.isEmpty()
                    ? averageSprite(upQuads.get(0).getSprite(), upQuads.get(0).getTintIndex())
                    : averageSprite(model.getParticleIcon(ModelData.EMPTY), 0);
        } catch (RuntimeException ignored) {
            result = TextureColor.EMPTY;
        }

        TEXTURE_COLOR_CACHE.put(state, result);
        return result;
    }

    private static TextureColor averageSprite(TextureAtlasSprite sprite, int tintIndex) {
        if (sprite == null || sprite.contents() == null) return TextureColor.EMPTY;

        int width = sprite.contents().width();
        int height = sprite.contents().height();
        int size = Math.min(width, height);
        if (size <= 0) return TextureColor.EMPTY;

        int step = Math.max(1, Math.min(4, size / 8));
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long count = 0L;

        for (int y = 0; y < size; y += step) {
            for (int x = 0; x < size; x += step) {
                int abgr = sprite.getPixelRGBA(0, x, y);
                int alpha = (abgr >> 24) & 0xFF;
                if (alpha == 0 || (abgr & 0x00FFFFFF) == 0) continue;

                blue += (abgr >> 16) & 0xFF;
                green += (abgr >> 8) & 0xFF;
                red += abgr & 0xFF;
                count++;
            }
        }

        if (count == 0L) return TextureColor.EMPTY;

        int rgb = ((int) (red / count) << 16) | ((int) (green / count) << 8) | (int) (blue / count);
        return new TextureColor(rgb, tintIndex);
    }

    private static int multiplyBiomeTint(int base, int tint) {
        int red = (((base >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int green = (((base >> 8) & 0xFF) * ((tint >> 8) & 0xFF)) / 255;
        int blue = ((base & 0xFF) * (tint & 0xFF)) / 255;
        return (red << 16) | (green << 8) | blue;
    }

    static int blendRgb(int bottom, int top, float topAlpha) {
        float bottomAlpha = 1.0f - topAlpha;
        int red = clampColor(Math.round(((bottom >> 16) & 0xFF) * bottomAlpha + ((top >> 16) & 0xFF) * topAlpha));
        int green = clampColor(Math.round(((bottom >> 8) & 0xFF) * bottomAlpha + ((top >> 8) & 0xFF) * topAlpha));
        int blue = clampColor(Math.round((bottom & 0xFF) * bottomAlpha + (top & 0xFF) * topAlpha));
        return (red << 16) | (green << 8) | blue;
    }

    private static int tuneTerrainRgb(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        float gray = red * 0.30f + green * 0.59f + blue * 0.11f;

        red = tuneChannel(red, gray);
        green = tuneChannel(green, gray);
        blue = tuneChannel(blue, gray);
        return (red << 16) | (green << 8) | blue;
    }

    private static int tuneChannel(int value, float gray) {
        float saturated = gray + (value - gray) * 1.08f;
        float contrasted = 128.0f + (saturated - 128.0f) * 1.04f;
        return clampColor(Math.round(contrasted));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}