package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;

final class WorldMapRegionTile {
    static final int CHUNKS_PER_REGION = 32;
    static final int PIXELS_PER_CHUNK = 16;
    static final int REGION_PIXEL_SIZE = CHUNKS_PER_REGION * PIXELS_PER_CHUNK;

    private final int regionX;
    private final int regionZ;
    private NativeImage image;
    private DynamicTexture texture;
    private ResourceLocation textureId;
    private boolean dirty;
    private boolean textureDirty;
    private long lastAccessNanos = System.nanoTime();

    WorldMapRegionTile(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    void loadOrCreate(File regionFile) {
        if (!loadFromFile(regionFile)) {
            closeImageAndTexture();
            this.image = new NativeImage(NativeImage.Format.RGBA, REGION_PIXEL_SIZE, REGION_PIXEL_SIZE, false);
            clearImage(this.image);
            this.dirty = false;
            this.textureDirty = true;
        }
    }

    boolean loadFromFile(File regionFile) {
        if (regionFile == null || !regionFile.exists() || regionFile.length() <= 0) return false;

        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(regionFile.toPath());
            NativeImage loadedImage = NativeImage.read(fileData);
            if (loadedImage.getWidth() != REGION_PIXEL_SIZE || loadedImage.getHeight() != REGION_PIXEL_SIZE) {
                loadedImage.close();
                return false;
            }

            closeImageAndTexture();
            this.image = loadedImage;
            this.dirty = false;
            this.textureDirty = true;
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    void loadFromImage(NativeImage loadedImage) {
        if (loadedImage == null) return;

        closeImageAndTexture();
        this.image = loadedImage;
        this.dirty = false;
        this.textureDirty = true;
    }

    void updateFromChunkImage(ChunkImage chunkImage, int chunkXInRegion, int chunkZInRegion) {
        if (this.image == null || chunkImage == null || !chunkImage.isMeaningful()) return;

        NativeImage chunkImg = chunkImage.getNativeImage();
        int startX = chunkXInRegion * PIXELS_PER_CHUNK;
        int startZ = chunkZInRegion * PIXELS_PER_CHUNK;

        for (int z = 0; z < PIXELS_PER_CHUNK; z++) {
            for (int x = 0; x < PIXELS_PER_CHUNK; x++) {
                this.image.setPixelRGBA(startX + x, startZ + z, chunkImg.getPixelRGBA(x, z));
            }
        }

        this.dirty = true;
        this.textureDirty = true;
    }

    void saveToFile(File regionFile) {
        if (this.image == null || !this.dirty) return;

        try {
            regionFile.getParentFile().mkdirs();
            this.image.writeToFile(regionFile);
            this.dirty = false;
        } catch (IOException ignored) {
        }
    }

    ResourceLocation getTextureId() {
        uploadTextureIfNeeded();
        return this.textureId;
    }

    NativeImage getImage() {
        return this.image;
    }

    int getRegionX() {
        return regionX;
    }

    int getRegionZ() {
        return regionZ;
    }

    void markAccessed() {
        this.lastAccessNanos = System.nanoTime();
    }

    long getLastAccessNanos() {
        return lastAccessNanos;
    }

    boolean isDirty() {
        return dirty;
    }

    void close() {
        closeImageAndTexture();
    }

    static int chunkToRegionCoord(int chunkCoord) {
        return Math.floorDiv(chunkCoord, CHUNKS_PER_REGION);
    }

    static int chunkLocalCoord(int chunkCoord) {
        return Math.floorMod(chunkCoord, CHUNKS_PER_REGION);
    }

    private void uploadTextureIfNeeded() {
        if (this.image == null) return;

        if (this.texture == null) {
            this.texture = new DynamicTexture(this.image);
            this.textureId = Minecraft.getInstance().getTextureManager()
                    .register("recruits_worldmap_region_" + regionX + "_" + regionZ, this.texture);
            this.textureDirty = false;
            return;
        }

        if (this.textureDirty) {
            this.texture.upload();
            this.textureDirty = false;
        }
    }

    private void closeImageAndTexture() {
        if (this.textureId != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureId);
        }
        try {
            if (this.texture != null) this.texture.close();
        } catch (Exception ignored) {
        }
        try {
            if (this.image != null) this.image.close();
        } catch (Exception ignored) {
        }
        this.image = null;
        this.texture = null;
        this.textureId = null;
    }

    private static void clearImage(NativeImage image) {
        for (int y = 0; y < REGION_PIXEL_SIZE; y++) {
            for (int x = 0; x < REGION_PIXEL_SIZE; x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }
}
