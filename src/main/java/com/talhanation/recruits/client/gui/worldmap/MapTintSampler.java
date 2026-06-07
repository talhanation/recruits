package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

final class MapTintSampler implements BlockAndTintGetter {
    private static final int MAX_BIOME_CACHE_ENTRIES = 16_384;
    private static final int NO_BIOME_COLOR = Integer.MIN_VALUE + 1;
    private static final int BIOME_COLOR_CACHE_MISS = Integer.MIN_VALUE;

    private final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    private final Reference2ObjectOpenHashMap<ColorResolver, Long2IntOpenHashMap> biomeColorCache =
            new Reference2ObjectOpenHashMap<>();
    private ChunkSamplingContext context;
    private ClientLevel cacheLevel;
    private BlockState tintState;
    private int biomeColorCacheEntries;

    void prepare(ChunkSamplingContext context) {
        ClientLevel level = context.level();
        if (cacheLevel != level || biomeColorCacheEntries > MAX_BIOME_CACHE_ENTRIES) {
            clearBiomeColorCache();
            cacheLevel = level;
        }
        this.context = context;
    }

    void clear() {
        context = null;
        tintState = null;
    }

    void useTintState(BlockState tintState) {
        this.tintState = tintState;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        ClientLevel level = level();
        return level == null ? null : level.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (tintState != null) return tintState;

        ClientLevel level = level();
        return level == null ? null : level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (tintState != null) return tintState.getFluidState();

        ClientLevel level = level();
        return level == null ? null : level.getFluidState(pos);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1.0f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        ClientLevel level = level();
        return level == null ? null : level.getLightEngine();
    }

    @Override
    public int getHeight() {
        ClientLevel level = level();
        return level == null ? 0 : level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        ClientLevel level = level();
        return level == null ? 0 : level.getMinBuildHeight();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        if (context == null) return NO_BIOME_COLOR;

        ClientLevel level = context.level();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 && dz != 0) continue;

                int sampleX = x + dx;
                int sampleZ = z + dz;
                if (context.getLoadedChunk(sampleX, sampleZ) == null) continue;

                int color = cachedBiomeColor(level, resolver, sampleX, y, sampleZ);
                red += (color >>> 16) & 0xFF;
                green += (color >>> 8) & 0xFF;
                blue += color & 0xFF;
                count++;
            }
        }

        if (count == 0) return NO_BIOME_COLOR;
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }

    private int cachedBiomeColor(ClientLevel level, ColorResolver resolver, int x, int y, int z) {
        Long2IntOpenHashMap resolverCache = biomeColorCache.get(resolver);
        if (resolverCache == null) {
            resolverCache = new Long2IntOpenHashMap();
            resolverCache.defaultReturnValue(BIOME_COLOR_CACHE_MISS);
            biomeColorCache.put(resolver, resolverCache);
        }

        long key = xzKey(x, z);
        int cached = resolverCache.get(key);
        if (cached != BIOME_COLOR_CACHE_MISS) return cached;

        mutable.set(x, y, z);
        int color = resolver.getColor(level.getBiome(mutable).value(), x, z);
        resolverCache.put(key, color);
        biomeColorCacheEntries++;
        return color;
    }

    private static long xzKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private void clearBiomeColorCache() {
        biomeColorCache.clear();
        biomeColorCacheEntries = 0;
    }

    static boolean hasColor(int color) {
        return color != -1 && color != NO_BIOME_COLOR && (color & 0x00FFFFFF) != 0;
    }

    private ClientLevel level() {
        return context == null ? null : context.level();
    }
}
