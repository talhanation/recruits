package com.talhanation.recruits.client.gui.worldmap.color;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * Small BlockAndTintGetter wrapper for saved map samples.
 */
public final class MapSourceTintSampler implements BlockAndTintGetter, MapTintStateOverride {
    private static final int NO_BIOME_COLOR = Integer.MIN_VALUE + 1;

    private final ClientLevel level;
    private final Registry<Biome> biomeRegistry;
    private final BiomeLookup biomeLookup;
    private BlockState tintState;

    public MapSourceTintSampler(ClientLevel level, BiomeLookup biomeLookup) {
        this.level = level;
        this.biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        this.biomeLookup = biomeLookup;
    }

    @Override
    public void useTintState(BlockState tintState) {
        this.tintState = tintState;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return tintState != null ? tintState : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1.0F;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        int x = pos.getX();
        int z = pos.getZ();
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;

        // Same cross-shaped blend used by the live sampler.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 && dz != 0) continue;

                int sampleX = x + dx;
                int sampleZ = z + dz;
                int biomeId = biomeLookup.biomeIdAt(sampleX, sampleZ);
                if (biomeId < 0) continue;

                Biome biome = biomeRegistry.byId(biomeId);
                if (biome == null) continue;

                int color = resolver.getColor(biome, sampleX, sampleZ);
                red += (color >>> 16) & 0xFF;
                green += (color >>> 8) & 0xFF;
                blue += color & 0xFF;
                count++;
            }
        }

        if (count == 0) return NO_BIOME_COLOR;
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }

    @FunctionalInterface
    public interface BiomeLookup {
        int biomeIdAt(int x, int z);
    }
}
