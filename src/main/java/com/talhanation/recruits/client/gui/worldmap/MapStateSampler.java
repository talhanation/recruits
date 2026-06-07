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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

final class MapStateSampler {
    private static final float ICE_OVERLAY_ALPHA = 0.52f;
    private static final float TRANSPARENT_OVERLAY_ALPHA = 0.50f;
    private static final Object TRAIT_CACHE_LOCK = new Object();
    private static final Map<BlockState, StateTraits> STATE_TRAIT_CACHE = new IdentityHashMap<>();
    private static final Set<BlockState> PENDING_STATE_TRAITS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static int traitCacheGeneration;

    private MapStateSampler() {
    }

    static void clearCaches() {
        synchronized (TRAIT_CACHE_LOCK) {
            STATE_TRAIT_CACHE.clear();
            PENDING_STATE_TRAITS.clear();
            traitCacheGeneration++;
        }
    }

    static boolean isWaterLike(BlockState state) {
        return state.getFluidState().is(Fluids.WATER);
    }

    static boolean isTransparentOverlay(ClientLevel level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || isWaterLike(state)) return false;

        return traits(state).transparentOverlay();
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

        return traits(state).renderable();
    }

    static boolean needsTraitWarmup(BlockState state) {
        if (state == null || state.isAir()) return false;

        synchronized (TRAIT_CACHE_LOCK) {
            return !STATE_TRAIT_CACHE.containsKey(state);
        }
    }

    static void requestTraitWarmup(BlockState state) {
        if (state == null || state.isAir()) return;

        int generation;
        synchronized (TRAIT_CACHE_LOCK) {
            if (STATE_TRAIT_CACHE.containsKey(state) || !PENDING_STATE_TRAITS.add(state)) return;

            generation = traitCacheGeneration;
        }

        WorldMapAsync.warmBlockColor(() -> {
            StateTraits traits = computeTraits(state);
            synchronized (TRAIT_CACHE_LOCK) {
                PENDING_STATE_TRAITS.remove(state);
                if (traitCacheGeneration == generation) {
                    STATE_TRAIT_CACHE.put(state, traits);
                }
            }
        });
    }

    private static StateTraits traits(BlockState state) {
        synchronized (TRAIT_CACHE_LOCK) {
            StateTraits cached = STATE_TRAIT_CACHE.get(state);
            if (cached != null) return cached;
        }

        StateTraits computed = computeTraits(state);
        synchronized (TRAIT_CACHE_LOCK) {
            PENDING_STATE_TRAITS.remove(state);
            StateTraits cached = STATE_TRAIT_CACHE.get(state);
            if (cached != null) return cached;

            STATE_TRAIT_CACHE.put(state, computed);
            return computed;
        }
    }

    private static StateTraits computeTraits(BlockState state) {
        return new StateTraits(
                computeRenderableMapStateTrait(state),
                computeTransparentOverlayTrait(state)
        );
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

    private record StateTraits(boolean renderable, boolean transparentOverlay) {
    }

}
