package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.nio.IntBuffer;

/**
 * Streams tile pixels through orphaned PBOs so texture updates do not wait for
 * the GPU to finish using an older upload buffer.
 */
final class WorldMapTextureUploader implements AutoCloseable {
    private static final int BUFFER_COUNT = 3;
    private static final int PIXEL_COUNT =
            WorldMapRenderTileKey.PIXEL_SIZE * WorldMapRenderTileKey.PIXEL_SIZE;
    private static final long BUFFER_BYTES = PIXEL_COUNT * Integer.BYTES;

    private final int[] pixelBuffers = new int[BUFFER_COUNT];
    private final IntBuffer stagingBuffer = BufferUtils.createIntBuffer(PIXEL_COUNT);
    private int nextBuffer;
    private boolean initialized;

    void prepare() {
        RenderSystem.assertOnRenderThread();
        ensureInitialized();
    }

    void upload(int textureId, int xOffset, int yOffset, int[] pixels) {
        uploadRegion(
                textureId,
                xOffset,
                yOffset,
                pixels,
                WorldMapRenderTileKey.PIXEL_SIZE,
                0,
                0,
                WorldMapRenderTileKey.PIXEL_SIZE,
                WorldMapRenderTileKey.PIXEL_SIZE
        );
    }

    void uploadRegion(int textureId, int xOffset, int yOffset, int[] pixels, int sourceWidth,
                      int sourceX, int sourceY, int width, int height) {
        if (pixels == null || pixels.length != PIXEL_COUNT) {
            throw new IllegalArgumentException("Invalid world map render tile pixel count");
        }
        if (sourceWidth != WorldMapRenderTileKey.PIXEL_SIZE
                || sourceX < 0 || sourceY < 0 || width <= 0 || height <= 0
                || sourceX + width > sourceWidth
                || sourceY + height > WorldMapRenderTileKey.PIXEL_SIZE) {
            throw new IllegalArgumentException("Invalid world map render tile upload area");
        }

        RenderSystem.assertOnRenderThread();
        ensureInitialized();

        stagingBuffer.clear();
        for (int row = 0; row < height; row++) {
            stagingBuffer.put(pixels, (sourceY + row) * sourceWidth + sourceX, width);
        }
        stagingBuffer.flip();

        int pixelBuffer = pixelBuffers[nextBuffer];
        nextBuffer = (nextBuffer + 1) % pixelBuffers.length;
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pixelBuffer);
        try {
            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, BUFFER_BYTES, GL15.GL_STREAM_DRAW);
            GL15.glBufferSubData(GL21.GL_PIXEL_UNPACK_BUFFER, 0L, stagingBuffer);

            RenderSystem.bindTexture(textureId);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    xOffset,
                    yOffset,
                    width,
                    height,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    0L
            );
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        }
    }

    @Override
    public void close() {
        if (!initialized) return;

        RenderSystem.assertOnRenderThread();
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        for (int pixelBuffer : pixelBuffers) {
            if (pixelBuffer != 0) {
                GL15.glDeleteBuffers(pixelBuffer);
            }
        }
        initialized = false;
        nextBuffer = 0;
    }

    private void ensureInitialized() {
        if (initialized) return;

        for (int index = 0; index < pixelBuffers.length; index++) {
            int pixelBuffer = GL15.glGenBuffers();
            pixelBuffers[index] = pixelBuffer;
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pixelBuffer);
            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, BUFFER_BYTES, GL15.GL_STREAM_DRAW);
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        initialized = true;
    }
}
