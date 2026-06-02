package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class WorldMapLodTile {
    private final int tileX;
    private final int tileZ;
    private final int sampleStep;
    private CompletableFuture<NativeImage> imageFuture;
    private NativeImage image;
    private DynamicTexture texture;
    private ResourceLocation textureId;
    private boolean failed;
    private long lastAccessNanos = System.nanoTime();

    WorldMapLodTile(int tileX, int tileZ, int sampleStep) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.sampleStep = sampleStep;
    }

    void schedule(Supplier<NativeImage> imageSupplier) {
        if (this.imageFuture != null || this.image != null || this.failed) return;
        this.imageFuture = WorldMapAsync.buildLod(imageSupplier);
    }

    boolean isScheduled() {
        return this.imageFuture != null;
    }

    ResourceLocation getTextureId() {
        uploadIfReady();
        return this.textureId;
    }

    int getSampleStep() {
        return sampleStep;
    }

    int getWorldSize() {
        return WorldMapRegionTile.REGION_PIXEL_SIZE * sampleStep;
    }

    int getWorldMinX() {
        return tileX * getWorldSize();
    }

    int getWorldMinZ() {
        return tileZ * getWorldSize();
    }

    int getWorldMaxX() {
        return getWorldMinX() + getWorldSize();
    }

    int getWorldMaxZ() {
        return getWorldMinZ() + getWorldSize();
    }

    void markAccessed() {
        this.lastAccessNanos = System.nanoTime();
    }

    long getLastAccessNanos() {
        return lastAccessNanos;
    }

    void close() {
        if (this.imageFuture != null) {
            CompletableFuture<NativeImage> future = this.imageFuture;
            if (future.isDone()) {
                closeFutureImage(future);
            } else {
                future.thenAccept(image -> {
                    if (image != null) image.close();
                });
            }
        }
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
        this.imageFuture = null;
        this.image = null;
        this.texture = null;
        this.textureId = null;
    }

    private void closeFutureImage(CompletableFuture<NativeImage> future) {
        try {
            NativeImage pendingImage = future.getNow(null);
            if (pendingImage != null && pendingImage != this.image) {
                pendingImage.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void uploadIfReady() {
        if (this.textureId != null || this.failed || this.imageFuture == null || !this.imageFuture.isDone()) return;

        NativeImage loadedImage;
        try {
            loadedImage = this.imageFuture.join();
        } catch (Exception ignored) {
            this.failed = true;
            this.imageFuture = null;
            return;
        }

        if (loadedImage == null) {
            this.failed = true;
            this.imageFuture = null;
            return;
        }

        this.image = loadedImage;
        this.texture = new DynamicTexture(this.image);
        this.textureId = Minecraft.getInstance().getTextureManager()
                .register("recruits_worldmap_lod_" + sampleStep + "_" + tileX + "_" + tileZ, this.texture);
        this.imageFuture = null;
    }
}
