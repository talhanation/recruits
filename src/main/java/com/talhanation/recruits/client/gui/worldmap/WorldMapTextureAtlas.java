package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps many map tiles in one persistent texture. Besides avoiding texture
 * allocation churn, this lets the renderer submit one batch per atlas.
 */
final class WorldMapTextureAtlas implements AutoCloseable {
    static final int ATLAS_SIZE = 1024;
    private static final int SLOTS_PER_SIDE = ATLAS_SIZE / WorldMapRenderTileKey.PIXEL_SIZE;
    private static final int CAPACITY = SLOTS_PER_SIDE * SLOTS_PER_SIDE;
    private static final AtomicLong ATLAS_SEQUENCE = new AtomicLong();

    private final PixelTexture texture = new PixelTexture();
    private final ResourceLocation textureId = new ResourceLocation(
            Main.MOD_ID,
            "worldmap/atlas_" + ATLAS_SEQUENCE.incrementAndGet()
    );
    private final BitSet usedSlots = new BitSet(CAPACITY);

    WorldMapTextureAtlas() {
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
    }

    Slot allocate() {
        int index = usedSlots.nextClearBit(0);
        if (index >= CAPACITY) return null;

        usedSlots.set(index);
        return new Slot(this, index);
    }

    void release(Slot slot) {
        if (slot != null && slot.atlas == this) {
            usedSlots.clear(slot.index);
        }
    }

    void upload(Slot slot, int[] pixels, WorldMapTextureUploader uploader) {
        uploadRegion(
                slot,
                pixels,
                0,
                0,
                WorldMapRenderTileKey.PIXEL_SIZE,
                WorldMapRenderTileKey.PIXEL_SIZE,
                uploader
        );
    }

    void uploadRegion(Slot slot, int[] pixels, int x, int y, int width, int height,
                      WorldMapTextureUploader uploader) {
        if (slot == null || slot.atlas != this || !usedSlots.get(slot.index)) {
            throw new IllegalStateException("Invalid world map atlas slot");
        }
        uploader.uploadRegion(
                texture.getId(),
                slot.pixelX() + x,
                slot.pixelY() + y,
                pixels,
                WorldMapRenderTileKey.PIXEL_SIZE,
                x,
                y,
                width,
                height
        );
    }

    ResourceLocation textureId() {
        return textureId;
    }

    boolean isEmpty() {
        return usedSlots.isEmpty();
    }

    @Override
    public void close() {
        usedSlots.clear();
        try {
            Minecraft.getInstance().getTextureManager().release(textureId);
        } catch (Exception ignored) {
            texture.close();
        }
    }

    record Slot(WorldMapTextureAtlas atlas, int index) {
        private int pixelX() {
            return (index % SLOTS_PER_SIDE) * WorldMapRenderTileKey.PIXEL_SIZE;
        }

        private int pixelY() {
            return (index / SLOTS_PER_SIDE) * WorldMapRenderTileKey.PIXEL_SIZE;
        }

        float u1() {
            return (float) pixelX() / ATLAS_SIZE;
        }

        float v1() {
            return (float) pixelY() / ATLAS_SIZE;
        }

        float u2() {
            return (float) (pixelX() + WorldMapRenderTileKey.PIXEL_SIZE) / ATLAS_SIZE;
        }

        float v2() {
            return (float) (pixelY() + WorldMapRenderTileKey.PIXEL_SIZE) / ATLAS_SIZE;
        }
    }

    private static final class PixelTexture extends AbstractTexture {
        private PixelTexture() {
            RenderSystem.assertOnRenderThreadOrInit();
            TextureUtil.prepareImage(getId(), ATLAS_SIZE, ATLAS_SIZE);
            setFilter(false, false);
            RenderSystem.bindTexture(getId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }

        @Override
        public void load(ResourceManager resourceManager) throws IOException {
        }

        @Override
        public void close() {
            releaseId();
        }
    }
}
