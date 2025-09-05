package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
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
import org.lwjgl.opengl.GL11;

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

    private ChunkMiniMap(ChunkPos pos, NativeImage img, DynamicTexture tex, ResourceLocation id) {
        this.chunkPos = pos;
        this.image = img;
        this.texture = tex;
        this.textureId = id;
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


    public void draw(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        // Textur binden
        RenderSystem.setShaderTexture(0, textureId);

        // Nearest-Neighbor erzwingen (kein Blurring)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        // Kleine Überlappung gegen Lücken zwischen Chunks
        int drawWidth = width + 1;
        int drawHeight = height + 1;

        // UV-Koordinaten mit minimalem Offset (gegen „falsches Pixelziehen“ am Rand)
        float uvFix = 0.001f;
        guiGraphics.blit(
                textureId,
                x, y,
                drawWidth, drawHeight,
                uvFix, uvFix,         // U/V-Start
                (int) (16 - 2 * uvFix),       // U-Breite
                (int) (16 - 2 * uvFix),       // V-Höhe
                16, 16                // Texturgröße
        );

        if (hovered) {
            guiGraphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
        }
    }


    public NativeImage getNativeImage() {
        return this.image;
    }

    public void close() {
        try {
            if (image != null) image.close();
        } catch (Exception ignored) {}
        try {
            if (texture != null) texture.close();
        } catch (Exception ignored) {}
        if (textureId != null) {
            mc.getTextureManager().release(textureId);
        }
    }

    // optional: konstruktor von NativeImage (laden aus file) -> du kannst einen neuen ctor machen:
    public static ChunkMiniMap fromNativeImage(ClientLevel level, ChunkPos pos, NativeImage img) {
        Minecraft mc = Minecraft.getInstance();
        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation id = mc.getTextureManager().register("chunk_map_" + pos.x + "_" + pos.z, tex);
        ChunkMiniMap cm = new ChunkMiniMap(pos, img, tex, id);
        return cm;
    }
}
