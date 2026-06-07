package com.talhanation.recruits.client.gui.worldmap.render.tile;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;

final class WorldMapRenderTile {
    private final WorldMapRenderTileKey key;
    private final WorldMapTextureAtlas.Slot slot;
    private final int[] pixels;
    private int sourceVersion;
    private long lastAccessOrder;
    private int dirtyMinX = WorldMapRenderTileKey.PIXEL_SIZE;
    private int dirtyMinZ = WorldMapRenderTileKey.PIXEL_SIZE;
    private int dirtyMaxX;
    private int dirtyMaxZ;

    private WorldMapRenderTile(
            WorldMapRenderTileKey key, int sourceVersion, WorldMapTextureAtlas.Slot slot, int[] pixels) {
        this.key = key;
        this.sourceVersion = sourceVersion;
        this.slot = slot;
        this.pixels = pixels;
    }

    static WorldMapRenderTile publish(
            WorldMapRenderTileKey key,
            WorldMapRegion.RenderSnapshot snapshot,
            WorldMapTextureAtlas.Slot slot,
            WorldMapTextureUploader uploader) {
        int[] pixels = snapshot.takePixels();
        if (pixels == null) {
            throw new IllegalStateException("World map render snapshot was already consumed");
        }
        try {
            slot.atlas().upload(slot, pixels, uploader);
            return new WorldMapRenderTile(key, snapshot.sourceVersion(), slot, pixels);
        } catch (RuntimeException | Error exception) {
            WorldMapRegion.releaseRenderPixels(pixels);
            throw exception;
        }
    }

    WorldMapRenderTileKey key() {
        return key;
    }

    int sourceVersion() {
        return sourceVersion;
    }

    void update(WorldMapRegion.RenderSnapshot snapshot, WorldMapTextureUploader uploader) {
        System.arraycopy(snapshot.pixels(), 0, pixels, 0, pixels.length);
        slot.atlas().upload(slot, pixels, uploader);
        sourceVersion = snapshot.sourceVersion();
        clearDirty();
    }

    void patchChunk(
            int[] chunkPixels, int chunkXInRegion, int chunkZInRegion, int sourceVersion) {
        int scale = 1 << key.level();
        int patchSize = WorldMapRegion.PIXELS_PER_CHUNK / scale;
        int targetX =
                (chunkXInRegion * WorldMapRegion.PIXELS_PER_CHUNK / scale)
                        % WorldMapRenderTileKey.PIXEL_SIZE;
        int targetZ =
                (chunkZInRegion * WorldMapRegion.PIXELS_PER_CHUNK / scale)
                        % WorldMapRenderTileKey.PIXEL_SIZE;

        for (int patchZ = 0; patchZ < patchSize; patchZ++) {
            int targetRow = (targetZ + patchZ) * WorldMapRenderTileKey.PIXEL_SIZE + targetX;
            for (int patchX = 0; patchX < patchSize; patchX++) {
                pixels[targetRow + patchX] =
                        averageArea(
                                chunkPixels,
                                WorldMapRegion.PIXELS_PER_CHUNK,
                                patchX * scale,
                                patchZ * scale,
                                scale);
            }
        }

        this.sourceVersion = sourceVersion;
        dirtyMinX = Math.min(dirtyMinX, targetX);
        dirtyMinZ = Math.min(dirtyMinZ, targetZ);
        dirtyMaxX = Math.max(dirtyMaxX, targetX + patchSize);
        dirtyMaxZ = Math.max(dirtyMaxZ, targetZ + patchSize);
    }

    boolean uploadPending(WorldMapTextureUploader uploader) {
        if (!isDirty()) return false;

        slot.atlas()
                .uploadRegion(
                        slot,
                        pixels,
                        dirtyMinX,
                        dirtyMinZ,
                        dirtyMaxX - dirtyMinX,
                        dirtyMaxZ - dirtyMinZ,
                        uploader);
        clearDirty();
        return true;
    }

    WorldMapTextureAtlas.Slot slot(long accessOrder) {
        lastAccessOrder = accessOrder;
        return slot;
    }

    long lastAccessOrder() {
        return lastAccessOrder;
    }

    void releaseSlot() {
        slot.atlas().release(slot);
        WorldMapRegion.releaseRenderPixels(pixels);
    }

    private boolean isDirty() {
        return dirtyMinX < dirtyMaxX && dirtyMinZ < dirtyMaxZ;
    }

    private void clearDirty() {
        dirtyMinX = WorldMapRenderTileKey.PIXEL_SIZE;
        dirtyMinZ = WorldMapRenderTileKey.PIXEL_SIZE;
        dirtyMaxX = 0;
        dirtyMaxZ = 0;
    }

    private static int averageArea(
            int[] source, int sourceSize, int startX, int startZ, int areaSize) {
        int alphaSum = 0;
        long redSum = 0L;
        long greenSum = 0L;
        long blueSum = 0L;
        for (int z = 0; z < areaSize; z++) {
            int row = (startZ + z) * sourceSize + startX;
            for (int x = 0; x < areaSize; x++) {
                int color = source[row + x];
                int alpha = color >>> 24;
                alphaSum += alpha;
                redSum += (color & 0xFFL) * alpha;
                greenSum += ((color >>> 8) & 0xFFL) * alpha;
                blueSum += ((color >>> 16) & 0xFFL) * alpha;
            }
        }

        if (alphaSum == 0) return 0;
        int sampleCount = areaSize * areaSize;
        int alpha = (alphaSum + sampleCount / 2) / sampleCount;
        int red = (int) ((redSum + alphaSum / 2L) / alphaSum);
        int green = (int) ((greenSum + alphaSum / 2L) / alphaSum);
        int blue = (int) ((blueSum + alphaSum / 2L) / alphaSum);
        return alpha << 24 | blue << 16 | green << 8 | red;
    }
}
