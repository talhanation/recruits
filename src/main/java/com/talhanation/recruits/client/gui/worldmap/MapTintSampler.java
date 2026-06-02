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

final class MapTintSampler implements BlockAndTintGetter {
    private ClientLevel level;

    MapTintSampler use(ClientLevel level) {
        this.level = level;
        return this;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return level.getFluidState(pos);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1.0f;
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
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
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

                mutable.set(x + dx, y, z + dz);
                var biomeHolder = level.getBiome(mutable);
                if (biomeHolder == null) continue;

                int color = resolver.getColor(biomeHolder.value(), mutable.getX(), mutable.getZ());
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
                count++;
            }
        }

        if (count == 0) return -1;
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }
}