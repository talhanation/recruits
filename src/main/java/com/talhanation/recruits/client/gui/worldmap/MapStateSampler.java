package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;

final class MapStateSampler {
    private static final float ICE_OVERLAY_ALPHA = 0.52f;
    private static final float TRANSPARENT_OVERLAY_ALPHA = 0.50f;
    private static final Map<BlockState, Boolean> RENDERABLE_STATE_TRAIT_CACHE = new HashMap<>();
    private static final Map<BlockState, Boolean> TRANSPARENT_OVERLAY_TRAIT_CACHE = new HashMap<>();

    private MapStateSampler() {
    }

    static void clearCaches() {
        RENDERABLE_STATE_TRAIT_CACHE.clear();
        TRANSPARENT_OVERLAY_TRAIT_CACHE.clear();
    }

    static boolean isWaterLike(BlockState state) {
        return state.getFluidState().is(Fluids.WATER);
    }

    static boolean isTransparentOverlay(ClientLevel level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || isWaterLike(state)) return false;

        boolean transparent = TRANSPARENT_OVERLAY_TRAIT_CACHE.computeIfAbsent(
                state, MapStateSampler::computeTransparentOverlayTrait
        );
        if (!transparent) return false;

        MapColor mapColor = state.getMapColor(level, pos);
        return mapColor != null && mapColor.col != 0;
    }

    private static boolean computeTransparentOverlayTrait(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE) return false;

        if (block instanceof GlassBlock) return true;
        return hasTranslucentRenderType(state);
    }

    static float getTransparentOverlayAlpha(BlockState state) {
        return state.getBlock() instanceof IceBlock ? ICE_OVERLAY_ALPHA : TRANSPARENT_OVERLAY_ALPHA;
    }

    static boolean isRenderableMapState(ClientLevel level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) return false;

        boolean renderable = RENDERABLE_STATE_TRAIT_CACHE.computeIfAbsent(
                state, MapStateSampler::computeRenderableMapStateTrait
        );
        if (!renderable || !state.getFluidState().isEmpty()) return renderable;

        MapColor mapColor = state.getMapColor(level, pos);
        return mapColor != null && mapColor.col != 0;
    }

    private static boolean computeRenderableMapStateTrait(BlockState state) {
        if (!state.getFluidState().isEmpty()) return true;

        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.TORCH || block == Blocks.GLASS || block == Blocks.GLASS_PANE) {
            return false;
        }

        return state.getRenderShape() != RenderShape.INVISIBLE;
    }

    private static boolean hasTranslucentRenderType(BlockState state) {
        try {
            return ItemBlockRenderTypes.getRenderLayers(state).contains(RenderType.translucent());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

}
