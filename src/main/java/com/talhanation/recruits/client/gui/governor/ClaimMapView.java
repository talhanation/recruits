package com.talhanation.recruits.client.gui.governor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.commandscreen.GovernorCategory;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapRegion;
import com.talhanation.recruits.governor.GovernorPost;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cut-out of the world map that shows only the chunks of the claim (no claim colour, no other claims,
 * no player marker), with the Governor posts on top.
 * <p>
 * The terrain is copied chunk by chunk from the world map data into an own small texture. A chunk keeps its
 * last picture while the world map rebuilds or reloads it, so the cut-out never flickers or turns black;
 * chunks the player never explored stay dark.
 */
@OnlyIn(Dist.CLIENT)
public class ClaimMapView {

    private static final int BACKGROUND = 0xFF1E1E1E;
    private static final int FRAME = 0xFF373737;
    private static final int BORDER = 0xFF101010;
    private static final long REFRESH_MILLIS = 1000L;
    private static int textureCounter;

    private final WorldMapCacheManager mapCache;
    @Nullable
    private DynamicTexture texture;
    @Nullable
    private ResourceLocation textureId;
    private final Set<Long> claimChunks = new HashSet<>();
    /** Chunks whose terrain is in the texture already. */
    private final Set<Long> filled = new HashSet<>();
    private int minChunkX;
    private int minChunkZ;
    private int chunksWide;
    private int chunksHigh;
    private long lastRefresh;
    private boolean waitingForRegions;
    private boolean closed;

    public ClaimMapView() {
        this.mapCache = WorldMapCacheManager.getInstance();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) mapCache.initialize(minecraft.level);
    }

    public void close() {
        closed = true;
        releaseTexture();
    }

    private void releaseTexture() {
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        texture = null;
        textureId = null;
        filled.clear();
    }

    /** Hovered post (for a tooltip), null if none. */
    @Nullable
    public GovernorPost render(GuiGraphics g, Font font, int x, int y, int width, int height,
                               List<ChunkPos> chunks, List<GovernorPost> posts, int mouseX, int mouseY) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, FRAME);
        g.fill(x, y, x + width, y + height, BACKGROUND);
        if (chunks.isEmpty() || closed) return null;

        prepareTexture(chunks);
        if (waitingForRegions) mapCache.beginRenderFrame(); // lets the world map finish loading saved regions
        refresh(chunks);
        if (texture == null || textureId == null) return null;

        int texWidth = chunksWide * 16;
        int texHeight = chunksHigh * 16;
        double scale = Math.min((double) width / texWidth, (double) height / texHeight);
        double left = x + (width - texWidth * scale) / 2D;
        double top = y + (height - texHeight * scale) / 2D;

        g.enableScissor(x, y, x + width, y + height);
        try {
            g.pose().pushPose();
            g.pose().translate(left, top, 0);
            g.pose().scale((float) scale, (float) scale, 1F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(textureId, 0, 0, 0, 0, texWidth, texHeight, texWidth, texHeight);
            RenderSystem.disableBlend();
            g.pose().popPose();

            renderBorder(g, left, top, scale);

            GovernorPost hovered = null;
            for (GovernorPost post : posts) {
                int px = (int) Math.floor(left + (post.getPos().getX() - minChunkX * 16 + 0.5D) * scale);
                int pz = (int) Math.floor(top + (post.getPos().getZ() - minChunkZ * 16 + 0.5D) * scale);
                g.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFF000000);
                g.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFF000000 | GovernorCategory.colorOf(post.getType()));
                if (Math.abs(mouseX - px) <= 2 && Math.abs(mouseY - pz) <= 2) hovered = post;
            }
            return hovered;
        } finally {
            g.disableScissor();
        }
    }

    /** Thin dark line around the claim, where a claim chunk meets a chunk outside of it. */
    private void renderBorder(GuiGraphics g, double left, double top, double scale) {
        for (long key : claimChunks) {
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            int x0 = (int) Math.round(left + (cx - minChunkX) * 16 * scale);
            int z0 = (int) Math.round(top + (cz - minChunkZ) * 16 * scale);
            int x1 = (int) Math.round(left + (cx - minChunkX + 1) * 16 * scale);
            int z1 = (int) Math.round(top + (cz - minChunkZ + 1) * 16 * scale);
            if (!claimChunks.contains(ChunkPos.asLong(cx, cz - 1))) g.fill(x0, z0, x1, z0 + 1, BORDER);
            if (!claimChunks.contains(ChunkPos.asLong(cx, cz + 1))) g.fill(x0, z1 - 1, x1, z1, BORDER);
            if (!claimChunks.contains(ChunkPos.asLong(cx - 1, cz))) g.fill(x0, z0, x0 + 1, z1, BORDER);
            if (!claimChunks.contains(ChunkPos.asLong(cx + 1, cz))) g.fill(x1 - 1, z0, x1, z1, BORDER);
        }
    }

    /** (Re)creates the texture when the claim got other chunks. */
    private void prepareTexture(List<ChunkPos> chunks) {
        Set<Long> keys = new HashSet<>();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : chunks) {
            keys.add(chunk.toLong());
            minX = Math.min(minX, chunk.x);
            minZ = Math.min(minZ, chunk.z);
            maxX = Math.max(maxX, chunk.x);
            maxZ = Math.max(maxZ, chunk.z);
        }
        if (texture != null && keys.equals(claimChunks)) return;

        releaseTexture();
        claimChunks.clear();
        claimChunks.addAll(keys);
        minChunkX = minX;
        minChunkZ = minZ;
        chunksWide = maxX - minX + 1;
        chunksHigh = maxZ - minZ + 1;
        texture = new DynamicTexture(chunksWide * 16, chunksHigh * 16, true);
        textureId = new ResourceLocation(Main.MOD_ID, "governor_claim_map_" + (textureCounter++));
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
        lastRefresh = 0L;
        waitingForRegions = true;
    }

    /** Copies the terrain of the claim chunks from the world map, once a second; missing chunks keep their old picture. */
    private void refresh(List<ChunkPos> chunks) {
        if (texture == null || texture.getPixels() == null) return;
        long now = Util.getMillis();
        if (now - lastRefresh < REFRESH_MILLIS) return;
        lastRefresh = now;

        NativeImage image = texture.getPixels();
        boolean changed = false;
        boolean waiting = false;
        for (ChunkPos chunk : chunks) {
            int regionX = WorldMapRegion.chunkToRegionCoord(chunk.x);
            int regionZ = WorldMapRegion.chunkToRegionCoord(chunk.z);
            WorldMapRegion region = mapCache.getLoadedRegion(regionX, regionZ);
            if (region == null) {
                region = mapCache.getOrSchedulePersistedRegion(regionX, regionZ, 0L, 0);
                if (region == null) {
                    waiting = true;
                    continue;
                }
            }
            int[] pixels = region.copyChunkPixels(WorldMapRegion.chunkLocalCoord(chunk.x), WorldMapRegion.chunkLocalCoord(chunk.z));
            if (pixels == null) continue;

            int baseX = (chunk.x - minChunkX) * 16;
            int baseZ = (chunk.z - minChunkZ) * 16;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int color = pixels[z * 16 + x];
                    // unexplored pixels of a chunk that was mapped before: keep what is there
                    if ((color >>> 24) == 0 && filled.contains(chunk.toLong())) continue;
                    image.setPixelRGBA(baseX + x, baseZ + z, color);
                }
            }
            filled.add(chunk.toLong());
            changed = true;
        }
        waitingForRegions = waiting;
        if (changed) texture.upload();
    }
}
