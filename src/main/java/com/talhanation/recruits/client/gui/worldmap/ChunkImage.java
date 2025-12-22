package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

public class ChunkImage {
    private final NativeImage image;
    private final DynamicTexture texture;
    private final ResourceLocation textureId;

    public ChunkImage(ClientLevel level, ChunkPos pos) {
        this.image = generateVanillaStyleImage(level, pos);
        this.texture = new DynamicTexture(image);
        this.textureId = Minecraft.getInstance().getTextureManager()
                .register("chunk_" + pos.x + "_" + pos.z, texture);
    }

    private NativeImage generateVanillaStyleImage(ClientLevel level, ChunkPos pos) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, 16, 16, true);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = pos.getMinBlockX() + x;
                int worldZ = pos.getMinBlockZ() + z;
                int worldY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                img.setPixelRGBA(x, z, getVanillaReliefColor(level, new BlockPos(worldX, worldY, worldZ)));
            }
        }
        img.untrack();
        return img;
    }

    private int getVanillaReliefColor(ClientLevel level, BlockPos pos) {

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return 0xFF000000;

        boolean isWaterLike = state.getFluidState().is(Fluids.WATER);

        if (isWaterLike) {

            BlockState topState = getTopWaterBlock(level, pos);
            MapColor mapColor = topState.getMapColor(level, pos);
            if (mapColor == null) mapColor = MapColor.WATER;

            int depth = getWaterDepth(level, pos);

            MapColor.Brightness brightness =
                    depth > 6 ? MapColor.Brightness.LOWEST :
                            depth > 3 ? MapColor.Brightness.LOW :
                                    MapColor.Brightness.NORMAL;

            return 0xFF000000 | mapColor.calculateRGBColor(brightness);
        }
        
        MapColor mapColor = state.getMapColor(level, pos);
        if (mapColor == null) return 0xFF000000;

        int heightHere = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        int heightSouth = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ() + 1);
        int heightWest  = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() - 1, pos.getZ());

        int relHeight = heightHere - Math.max(heightSouth, heightWest);

        MapColor.Brightness brightness =
                relHeight > 2  ? MapColor.Brightness.HIGH :
                        relHeight > 0  ? MapColor.Brightness.NORMAL :
                                relHeight > -2 ? MapColor.Brightness.LOW :
                                        MapColor.Brightness.LOWEST;

        return 0xFF000000 | mapColor.calculateRGBColor(brightness);
    }

    private BlockState getTopWaterBlock(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();

        while (isWaterLike(level.getBlockState(mutable))
                && mutable.getY() < level.getMaxBuildHeight()) {
            mutable.move(Direction.UP);
        }

        return level.getBlockState(mutable.below());
    }

    private int getWaterDepth(ClientLevel level, BlockPos pos) {
        int depth = 0;
        BlockPos.MutableBlockPos mutable = pos.mutable();

        while (isWaterLike(level.getBlockState(mutable))
                && mutable.getY() > level.getMinBuildHeight()) {
            depth++;
            mutable.move(Direction.DOWN);
        }

        return depth;
    }

    private boolean isWaterLike(BlockState state) {
        return state.getFluidState().is(Fluids.WATER);
    }

    public NativeImage getNativeImage() {
        return this.image;
    }

    public boolean isMeaningful() {
        if (this.image == null) return false;
        int meaningful = 0;
        for (int i = 0; i < 256; i++) {
            int pixel = this.image.getPixelRGBA(i % 16, i / 16);
            int alpha = (pixel >> 24) & 0xFF;
            int rgb = pixel & 0x00FFFFFF;
            if (alpha > 0 && rgb != 0) meaningful++;
        }
        return meaningful >= 25; // ~10% von 256
    }

    public void close() {
        try { if (image != null) image.close(); } catch (Exception ignored) {}
        try { if (texture != null) texture.close(); } catch (Exception ignored) {}
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
    }
}
