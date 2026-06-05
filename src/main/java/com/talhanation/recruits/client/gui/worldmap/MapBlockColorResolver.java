package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.data.ModelData;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class MapBlockColorResolver {
    private static final int WATER_BLUE = 0x1F5DA8;
    private static final int DEFAULT_TINT_INDEX = 0;
    private static final RandomSource MODEL_RANDOM = RandomSource.create(42L);
    private static final Map<BlockState, TextureColor> TEXTURE_COLOR_CACHE = new IdentityHashMap<>();
    private static final Map<TextureAtlasSprite, Integer> SPRITE_RGB_CACHE = new IdentityHashMap<>();

    private MapBlockColorResolver() {
    }

    static int resolveBaseRgb(ChunkSamplingContext context, BlockPos pos, BlockState state,
                              MapTintSampler tintSampler) {
        ClientLevel level = context.level();
        TextureColor textureColor = getTextureColor(state);
        int base = textureColor.rgb();
        if (base == 0) {
            MapColor mapColor = state.getMapColor(level, pos);
            base = mapColor != null ? mapColor.col : 0;
        }
        if ((base & 0x00FFFFFF) == 0) return 0;

        int tint = resolveBiomeTint(pos, state, textureColor.tintIndex(), tintSampler);
        if (MapTintSampler.hasColor(tint)) {
            base = multiplyBiomeTint(base, tint & 0x00FFFFFF);
        }

        return tuneTerrainRgb(base);
    }

    static void clearCaches() {
        TEXTURE_COLOR_CACHE.clear();
        SPRITE_RGB_CACHE.clear();
    }

    static int resolveWaterNativeColor(MapSample sample, MapSample floor, int shoreNeighbors, int relief) {
        int waterRgb = blendRgb(sample.baseRgb(), WATER_BLUE, 0.32f);
        if ((waterRgb & 0x00FFFFFF) == 0) return 0x00000000;

        int depth = 4;
        int blendedRgb = waterRgb;
        if (floor != null && floor.isPresent()) {
            int floorRgb = floor.baseRgb();
            if ((floorRgb & 0x00FFFFFF) != 0) {
                depth = Math.max(1, sample.height() - floor.height());
                float waterAlpha = clamp(0.48f + Math.min(depth, 7) * 0.065f, 0.52f, 0.84f);
                blendedRgb = blendRgb(floorRgb, waterRgb, waterAlpha);
            }
        }

        return applyBrightnessToNativeColor(
                blendedRgb,
                MapReliefShading.computeWaterBrightness(shoreNeighbors, depth, relief)
        );
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

    private static int resolveBiomeTint(BlockPos pos, BlockState state, int tintIndex, MapTintSampler tintSampler) {
        if (tintIndex < 0) return -1;

        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        tintSampler.useTintState(state);
        try {
            return blockColors.getColor(state, tintSampler, pos, tintIndex);
        } finally {
            tintSampler.useTintState(null);
        }
    }

    private static TextureColor getTextureColor(BlockState state) {
        TextureColor cached = TEXTURE_COLOR_CACHE.get(state);
        if (cached != null) return cached;

        long startNanos = System.nanoTime();
        TextureColor result;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BakedModel model = minecraft.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
            MODEL_RANDOM.setSeed(42L);
            List<BakedQuad> upQuads = model.getQuads(state, Direction.UP, MODEL_RANDOM, ModelData.EMPTY, null);
            BakedQuad quad = selectColorQuad(upQuads);
            TextureAtlasSprite sprite = quad != null ? quad.getSprite() : null;
            int tintIndex = quad != null ? quad.getTintIndex() : -1;
            if (quad == null || sprite == null || isMissingSprite(sprite)) {
                MODEL_RANDOM.setSeed(42L);
                List<BakedQuad> generalQuads = model.getQuads(state, null, MODEL_RANDOM, ModelData.EMPTY, null);
                quad = selectColorQuad(generalQuads);
                if (quad != null) {
                    sprite = quad.getSprite();
                    tintIndex = quad.getTintIndex();
                }
            }
            if (sprite == null || isMissingSprite(sprite)) {
                sprite = model.getParticleIcon(ModelData.EMPTY);
                tintIndex = DEFAULT_TINT_INDEX;
            }
            result = isMissingSprite(sprite) ? TextureColor.EMPTY : averageSprite(sprite, tintIndex);
        } catch (RuntimeException ignored) {
            result = TextureColor.EMPTY;
        }

        TEXTURE_COLOR_CACHE.put(state, result);
        WorldMapBuildProfiler.recordTextureColorMiss(
                System.nanoTime() - startNanos,
                String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
        );
        return result;
    }

    private static boolean isMissingSprite(TextureAtlasSprite sprite) {
        if (sprite == null) return true;
        TextureAtlasSprite missingSprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(MissingTextureAtlasSprite.getLocation());
        return sprite == missingSprite;
    }

    private static BakedQuad selectColorQuad(List<BakedQuad> quads) {
        BakedQuad fallback = null;
        for (BakedQuad quad : quads) {
            if (quad == null || isMissingSprite(quad.getSprite())) continue;
            if (quad.getTintIndex() >= 0) return quad;
            if (fallback == null) fallback = quad;
        }
        return fallback;
    }

    private static TextureColor averageSprite(TextureAtlasSprite sprite, int tintIndex) {
        if (sprite == null || sprite.contents() == null) return TextureColor.EMPTY;

        Integer cachedRgb = SPRITE_RGB_CACHE.get(sprite);
        if (cachedRgb != null) return new TextureColor(cachedRgb, tintIndex);

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
        SPRITE_RGB_CACHE.put(sprite, rgb);
        return new TextureColor(rgb, tintIndex);
    }

    private static int multiplyBiomeTint(int base, int tint) {
        int red = (((base >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int green = (((base >> 8) & 0xFF) * ((tint >> 8) & 0xFF)) / 255;
        int blue = ((base & 0xFF) * (tint & 0xFF)) / 255;
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

    static int blendRgb(int bottom, int top, float topAlpha) {
        float bottomAlpha = 1.0f - topAlpha;
        int red = clampColor(Math.round(((bottom >> 16) & 0xFF) * bottomAlpha + ((top >> 16) & 0xFF) * topAlpha));
        int green = clampColor(Math.round(((bottom >> 8) & 0xFF) * bottomAlpha + ((top >> 8) & 0xFF) * topAlpha));
        int blue = clampColor(Math.round((bottom & 0xFF) * bottomAlpha + (top & 0xFF) * topAlpha));
        return (red << 16) | (green << 8) | blue;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
