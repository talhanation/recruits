package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class WorldMapLodTile {
    private static final long FAILED_RETRY_DELAY_NANOS = 1_000_000_000L;

    private final int tileX;
    private final int tileZ;
    private final int sampleStep;
    private CompletableFuture<NativeImage> imageFuture;
    private NativeImage image;
    private DynamicTexture texture;
    private ResourceLocation textureId;
    private boolean failed;
    private boolean dirty;
    private long lastFailureNanos;
    private long lastAccessNanos = System.nanoTime();

    WorldMapLodTile(int tileX, int tileZ, int sampleStep) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.sampleStep = sampleStep;
    }

    boolean schedule(Supplier<NativeImage> imageSupplier) {
        if (this.imageFuture != null) return false;
        if (this.failed && System.nanoTime() - this.lastFailureNanos < FAILED_RETRY_DELAY_NANOS) return false;

        this.dirty = false;
        this.imageFuture = WorldMapAsync.buildLod(imageSupplier);
        return true;
    }

    boolean isScheduled() {
        return this.imageFuture != null;
    }

    boolean isBuildPending() {
        return this.imageFuture != null && !this.imageFuture.isDone();
    }

    boolean needsBuild() {
        return this.textureId == null || this.dirty;
    }

    void markDirty() {
        this.dirty = true;
        this.failed = false;
        this.lastFailureNanos = 0L;
    }

    ResourceLocation getTextureId(boolean allowUpload) {
        if (allowUpload) {
            uploadIfReady();
        }
        return this.textureId;
    }

    boolean hasUploadReady() {
        return !this.failed && this.imageFuture != null && this.imageFuture.isDone();
    }

    boolean hasTexture() {
        return this.textureId != null;
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
        if (this.failed || this.imageFuture == null || !this.imageFuture.isDone()) return;

        NativeImage loadedImage;
        try {
            loadedImage = this.imageFuture.join();
        } catch (Exception ignored) {
            this.imageFuture = null;
            markFailedIfEmpty();
            return;
        }

        if (loadedImage == null) {
            this.imageFuture = null;
            markFailedIfEmpty();
            return;
        }

        replaceTexture(loadedImage);
        this.failed = false;
        this.imageFuture = null;
    }

    private void replaceTexture(NativeImage loadedImage) {
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

        this.image = loadedImage;
        this.texture = new DynamicTexture(this.image);
        this.texture.setFilter(false, false);
        applyMapTextureSampling(this.texture.getId());
        this.textureId = Minecraft.getInstance().getTextureManager()
                .register("recruits_worldmap_lod_" + sampleStep + "_" + tileX + "_" + tileZ, this.texture);
    }

    private void markFailedIfEmpty() {
        if (this.textureId != null) {
            this.failed = false;
            return;
        }

        this.failed = true;
        this.lastFailureNanos = System.nanoTime();
    }

    private static void applyMapTextureSampling(int textureId) {
        RenderSystem.bindTexture(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }
}
