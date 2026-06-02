package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MapStateSampler {
    private static final float ICE_OVERLAY_ALPHA = 0.52f;
    private static final float TRANSPARENT_OVERLAY_ALPHA = 0.50f;
    private static final Map<BlockState, Boolean> TRANSPARENT_OVERLAY_CACHE = new ConcurrentHashMap<>();

    private MapStateSampler() {
    }

    static MapSample findTopMapSample(ClientLevel level, int worldX, int worldZ) {
        if (!isColumnLoaded(level, worldX, worldZ)) return null;

        int startY = Math.min(level.getMaxBuildHeight() - 1, getSurfaceHeight(level, worldX, worldZ) + 3);
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(worldX, startY, worldZ);

        for (int y = startY; y >= minY; y--) {
            mutable.setY(y);
            BlockState state = level.getBlockState(mutable);
            if (isRenderableMapState(level, mutable, state)) {
                return new MapSample(mutable.immutable(), state, y);
            }
        }

        return null;
    }

    static MapSample findUnderWaterSample(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        int minY = level.getMinBuildHeight();
        int scanned = 0;

        while (mutable.getY() > minY && scanned < 16) {
            mutable.move(Direction.DOWN);
            scanned++;
            BlockState state = level.getBlockState(mutable);
            if (state.isAir() || isWaterLike(state)) continue;

            if (isRenderableMapState(level, mutable, state)) {
                return new MapSample(mutable.immutable(), state, mutable.getY());
            }
        }

        return null;
    }

    static MapSample findUnderOverlaySample(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        int minY = level.getMinBuildHeight();
        int scanned = 0;

        while (mutable.getY() > minY && scanned < 32) {
            mutable.move(Direction.DOWN);
            scanned++;
            BlockState state = level.getBlockState(mutable);
            if (state.isAir() || isTransparentOverlay(level, mutable, state)) continue;

            if (isWaterLike(state) || isRenderableMapState(level, mutable, state)) {
                return new MapSample(mutable.immutable(), state, mutable.getY());
            }
        }

        return null;
    }

    static int getSurfaceHeight(ClientLevel level, int x, int z) {
        if (!isColumnLoaded(level, x, z)) return level.getMinBuildHeight();
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    static int getWaterDepth(ClientLevel level, BlockPos pos) {
        int depth = 0;
        BlockPos.MutableBlockPos mutable = pos.mutable();

        while (isWaterLike(level.getBlockState(mutable)) && mutable.getY() > level.getMinBuildHeight()) {
            depth++;
            mutable.move(Direction.DOWN);
        }

        return depth;
    }

    static int countWaterNeighbors(ClientLevel level, BlockPos pos) {
        int count = 0;
        if (isWaterLike(level.getBlockState(pos.north()))) count++;
        if (isWaterLike(level.getBlockState(pos.south()))) count++;
        if (isWaterLike(level.getBlockState(pos.east()))) count++;
        if (isWaterLike(level.getBlockState(pos.west()))) count++;
        return count;
    }

    static boolean isWaterLike(BlockState state) {
        return state.getFluidState().is(Fluids.WATER);
    }

    static boolean isTransparentOverlay(ClientLevel level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || isWaterLike(state)) return false;

        Block block = state.getBlock();
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) return false;

        MapColor mapColor = state.getMapColor(level, pos);
        if (mapColor == null || mapColor.col == 0) return false;

        if (block instanceof GlassBlock) return true;
        return TRANSPARENT_OVERLAY_CACHE.computeIfAbsent(state, MapStateSampler::hasTranslucentRenderType);
    }

    static float getTransparentOverlayAlpha(BlockState state) {
        return state.getBlock() instanceof IceBlock ? ICE_OVERLAY_ALPHA : TRANSPARENT_OVERLAY_ALPHA;
    }

    private static boolean isRenderableMapState(ClientLevel level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return true;

        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.TORCH || block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
            return false;
        }

        if (state.getRenderShape() == RenderShape.INVISIBLE) return false;

        MapColor mapColor = state.getMapColor(level, pos);
        return mapColor != null && mapColor.col != 0;
    }

    private static boolean hasTranslucentRenderType(BlockState state) {
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
            if (model == null) return true;
            return model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY).contains(RenderType.translucent());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isColumnLoaded(ClientLevel level, int worldX, int worldZ) {
        try {
            return level.getChunkSource().getChunk(worldX >> 4, worldZ >> 4, false) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
