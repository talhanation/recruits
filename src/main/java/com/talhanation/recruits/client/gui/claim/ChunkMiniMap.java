package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public class ChunkMiniMap {
    private final ChunkPos chunkPos;
    private final NativeImage image;
    private final DynamicTexture texture;
    private final ResourceLocation textureId;
    private final Minecraft mc = Minecraft.getInstance();

    public ChunkMiniMap(ClientLevel level, ChunkPos pos) {
        this.chunkPos = pos;
        this.image = generateVanillaStyleImage(level, pos);
        this.texture = new DynamicTexture(image);
        this.textureId = mc.getTextureManager().register("chunk_map_" + pos.x + "_" + pos.z, texture);
    }

    private NativeImage generateVanillaStyleImage(ClientLevel level, ChunkPos pos) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, 16, 16, false);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = pos.getMinBlockX() + x;
                int worldZ = pos.getMinBlockZ() + z;
                int worldY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                BlockPos blockPos = new BlockPos(worldX, worldY, worldZ);

                int color = getVanillaReliefColor(level, blockPos);
                img.setPixelRGBA(x, z, color);
            }
        }

        img.untrack();
        return img;
    }

    private int getVanillaReliefColor(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        MapColor mapColor = state.getMapColor(level, pos);
        if (mapColor == null) return 0xFF000000;

        int x = pos.getX();
        int z = pos.getZ();
        int y = pos.getY();

        // Prüfen ob Wasser an dieser Stelle
        boolean isWater = state.getBlock() == Blocks.WATER;

        if (isWater) {
            int waterDepth = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, y, z);
            while (level.getBlockState(mutable).getBlock() == Blocks.WATER && mutable.getY() > level.getMinBuildHeight()) {
                waterDepth++;
                mutable.move(Direction.DOWN);
            }

            // je tiefer das Wasser, desto dunkler
            MapColor.Brightness brightness;
            if (waterDepth > 6) {
                brightness = MapColor.Brightness.LOWEST;
            } else if (waterDepth > 3) {
                brightness = MapColor.Brightness.LOW;
            } else {
                brightness = MapColor.Brightness.NORMAL;
            }

            return 0xFF000000 | mapColor.calculateRGBColor(brightness);
        }

        // Relief-Berechnung wie bisher
        int heightHere = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int heightSouth = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z + 1);
        int heightWest = level.getHeight(Heightmap.Types.WORLD_SURFACE, x - 1, z);

        int relHeight = heightHere - Math.max(heightSouth, heightWest);

        MapColor.Brightness brightness;
        if (relHeight > 2) {
            brightness = MapColor.Brightness.HIGH;
        } else if (relHeight > 0) {
            brightness = MapColor.Brightness.NORMAL;
        } else if (relHeight > -2) {
            brightness = MapColor.Brightness.LOW;
        } else {
            brightness = MapColor.Brightness.LOWEST;
        }

        return 0xFF000000 | mapColor.calculateRGBColor(brightness);
    }


    public void draw(GuiGraphics gui, int screenX, int screenY, boolean highlight) {
        gui.blit(textureId, screenX, screenY, 0, 0, 16, 16, 16, 16);

        if (highlight) {
            gui.fill(screenX, screenY, screenX + 16, screenY + 16, 0x40FFFFFF); // halbtransparent weiß
        }
    }

    public void close() {
        image.close();
        texture.close();
        mc.getTextureManager().release(textureId); // wichtig, um Leaks zu vermeiden
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }
}
