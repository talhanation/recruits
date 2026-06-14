package com.talhanation.recruits.client.gui.worldmap.claim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.faction.FactionEditScreen;
import com.talhanation.recruits.client.gui.worldmap.claim.WorldMapClaimController.ClaimPreviewChunk;
import com.talhanation.recruits.client.gui.worldmap.render.MapRenderUtil;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.world.RecruitsClaim;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimRenderer {
    private static final int CLAIM_FILL_ALPHA = 160;
    private static final Map<UUID, ClaimShape> CLAIM_SHAPES = new HashMap<>();

    public static void invalidateShapeCache() {
        CLAIM_SHAPES.clear();
    }

    public static void renderClaimsOverlay(
            GuiGraphics guiGraphics, RecruitsClaim selectedClaim, double offsetX, double offsetZ, double scale) {
        if (ClientManager.recruitsClaims.isEmpty()) return;

        RenderBounds bounds = RenderBounds.fromScreen(offsetX, offsetZ, scale);
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimFill(guiGraphics, claim, offsetX, offsetZ, scale, bounds);
        }

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimPassiveOutline(guiGraphics, claim, offsetX, offsetZ, scale, bounds);
        }

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimName(guiGraphics, claim, offsetX, offsetZ, scale, bounds);
        }

        if (selectedClaim != null) {
            renderClaimSelectedOutline(guiGraphics, selectedClaim, offsetX, offsetZ, scale, bounds);
        }
    }

    /**
     * Renders claims with transparent fill so route waypoints underneath are visible.
     * Outlines and selected outline are kept intact; only the fill becomes invisible.
     */
    public static void renderClaimsOverlayTransparent(
            GuiGraphics guiGraphics, RecruitsClaim selectedClaim, double offsetX, double offsetZ, double scale) {
        if (ClientManager.recruitsClaims.isEmpty()) return;

        RenderBounds bounds = RenderBounds.fromScreen(offsetX, offsetZ, scale);
        // Skip fill; only draw outlines and selection so the claim boundaries stay visible.
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimPassiveOutline(guiGraphics, claim, offsetX, offsetZ, scale, bounds);
        }

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimName(guiGraphics, claim, offsetX, offsetZ, scale, bounds);
        }

        if (selectedClaim != null) {
            renderClaimSelectedOutline(guiGraphics, selectedClaim, offsetX, offsetZ, scale, bounds);
        }
    }

    private static final int FOG_FILL_COLOR = 0x30303030;
    private static final int FOG_OUTLINE_COLOR = 0x80555555;

    private static boolean isAdminCreative() {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2) && player.isCreative();
    }

    public static boolean isClaimExplored(RecruitsClaim claim) {
        if (claim == null || claim.getClaimedChunks() == null) return false;
        WorldMapCacheManager mapCache = WorldMapCacheManager.getInstance();
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (chunk == null) continue;
            if (mapCache.isChunkExplored(chunk)) return true;
        }
        return false;
    }

    private static void renderClaimFill(
            GuiGraphics guiGraphics,
            RecruitsClaim claim,
            double offsetX,
            double offsetZ,
            double scale,
            RenderBounds bounds) {
        ClaimShape shape = getShape(claim);
        if (shape == null || !bounds.intersects(shape)) return;

        int factionFillColor = (CLAIM_FILL_ALPHA << 24) | (getClaimColor(claim) & 0x00FFFFFF);
        boolean adminCreative = isAdminCreative();
        WorldMapCacheManager mapCache = WorldMapCacheManager.getInstance();

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (chunk == null) continue;
            boolean explored = adminCreative || mapCache.isChunkExplored(chunk);
            renderChunk(
                    guiGraphics,
                    chunk,
                    explored ? factionFillColor : FOG_FILL_COLOR,
                    offsetX,
                    offsetZ,
                    scale,
                    bounds);
        }
    }

    private static void renderChunk(
            GuiGraphics guiGraphics,
            ChunkPos chunk,
            int color,
            double offsetX,
            double offsetZ,
            double scale,
            RenderBounds bounds) {
        if (chunk == null) return;
        if (bounds != null && !bounds.contains(chunk)) return;

        double worldX = chunk.x * 16.0;
        double worldZ = chunk.z * 16.0;

        double x1 = offsetX + worldX * scale;
        double z1 = offsetZ + worldZ * scale;
        double x2 = offsetX + (worldX + 16.0) * scale;
        double z2 = offsetZ + (worldZ + 16.0) * scale;

        MapRenderUtil.fill(guiGraphics, x1, z1, x2, z2, color);
    }

    private static void renderClaimPassiveOutline(
            GuiGraphics guiGraphics,
            RecruitsClaim claim,
            double offsetX,
            double offsetZ,
            double scale,
            RenderBounds bounds) {
        ClaimShape shape = getShape(claim);
        if (shape == null || !bounds.intersects(shape)) return;

        int factionOutlineColor = (200 << 24) | (getClaimColor(claim) & 0x00FFFFFF);
        int thickness = Math.max(1, (int) Math.round(scale * 0.5));
        boolean adminCreative = isAdminCreative();
        WorldMapCacheManager mapCache = WorldMapCacheManager.getInstance();

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (chunk == null) continue;
            if (!bounds.contains(chunk)) continue;
            boolean explored =
                    adminCreative || !ClientManager.configFogOfWarEnabled || mapCache.isChunkExplored(chunk);
            int outlineColor = explored ? factionOutlineColor : FOG_OUTLINE_COLOR;

            boolean hasTop = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x, chunk.z - 1));
            boolean hasBottom = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x, chunk.z + 1));
            boolean hasLeft = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x - 1, chunk.z));
            boolean hasRight = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x + 1, chunk.z));

            double worldX1 = chunk.x * 16.0;
            double worldZ1 = chunk.z * 16.0;

            double x1 = offsetX + worldX1 * scale;
            double z1 = offsetZ + worldZ1 * scale;
            double x2 = offsetX + (worldX1 + 16.0) * scale;
            double z2 = offsetZ + (worldZ1 + 16.0) * scale;

            if (!hasTop) {
                MapRenderUtil.fill(guiGraphics, x1, z1, x2, z1 + thickness, outlineColor);
            }
            if (!hasBottom) {
                MapRenderUtil.fill(guiGraphics, x1, z2 - thickness, x2, z2, outlineColor);
            }
            if (!hasLeft) {
                MapRenderUtil.fill(guiGraphics, x1, z1, x1 + thickness, z2, outlineColor);
            }
            if (!hasRight) {
                MapRenderUtil.fill(guiGraphics, x2 - thickness, z1, x2, z2, outlineColor);
            }
        }

        renderConcaveBoundaryCornerCaps(
                guiGraphics,
                claim.getClaimedChunks(),
                shape.chunkSet,
                offsetX,
                offsetZ,
                scale,
                thickness,
                factionOutlineColor,
                FOG_OUTLINE_COLOR,
                adminCreative,
                mapCache,
                bounds);
    }

    private static void renderClaimSelectedOutline(
            GuiGraphics guiGraphics,
            RecruitsClaim claim,
            double offsetX,
            double offsetZ,
            double scale,
            RenderBounds bounds) {
        ClaimShape shape = getShape(claim);
        if (shape == null || !bounds.intersects(shape)) return;

        int borderColor = 0xFFFFFFFF;
        int borderThickness = Math.max(1, (int) (2 * scale / 2.0));

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (chunk == null) continue;
            if (!bounds.contains(chunk)) continue;
            boolean hasTop = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x, chunk.z - 1));
            boolean hasBottom = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x, chunk.z + 1));
            boolean hasLeft = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x - 1, chunk.z));
            boolean hasRight = shape.chunkSet.contains(WorldMapClaimIndex.chunkKey(chunk.x + 1, chunk.z));

            if (hasTop && hasBottom && hasLeft && hasRight) {
                continue;
            }

            double worldX1 = chunk.x * 16.0;
            double worldZ1 = chunk.z * 16.0;
            double worldX2 = worldX1 + 16.0;
            double worldZ2 = worldZ1 + 16.0;

            double x1 = offsetX + worldX1 * scale;
            double z1 = offsetZ + worldZ1 * scale;
            double x2 = offsetX + worldX2 * scale;
            double z2 = offsetZ + worldZ2 * scale;

            if (!hasTop) {
                MapRenderUtil.fill(guiGraphics, x1, z1, x2, z1 + borderThickness, borderColor);
            }
            if (!hasBottom) {
                MapRenderUtil.fill(guiGraphics, x1, z2 - borderThickness, x2, z2, borderColor);
            }
            if (!hasLeft) {
                MapRenderUtil.fill(guiGraphics, x1, z1, x1 + borderThickness, z2, borderColor);
            }
            if (!hasRight) {
                MapRenderUtil.fill(guiGraphics, x2 - borderThickness, z1, x2, z2, borderColor);
            }
        }

        renderConcaveBoundaryCornerCaps(
                guiGraphics,
                claim.getClaimedChunks(),
                shape.chunkSet,
                offsetX,
                offsetZ,
                scale,
                borderThickness,
                borderColor,
                borderColor,
                true,
                null,
                bounds);
    }

    private static void renderConcaveBoundaryCornerCaps(
            GuiGraphics guiGraphics,
            List<ChunkPos> claimedChunks,
            LongOpenHashSet chunkSet,
            double offsetX,
            double offsetZ,
            double scale,
            double thickness,
            int exploredColor,
            int unexploredColor,
            boolean adminCreative,
            WorldMapCacheManager mapCache,
            RenderBounds bounds) {
        LongOpenHashSet renderedCorners = new LongOpenHashSet();
        double capSize = Math.max(1.0, thickness);

        for (ChunkPos chunk : claimedChunks) {
            if (chunk == null) continue;
            if (!bounds.isNear(chunk, 1)) continue;

            renderConcaveBoundaryCornerCap(
                    guiGraphics,
                    chunkSet,
                    renderedCorners,
                    chunk.x,
                    chunk.z,
                    offsetX,
                    offsetZ,
                    scale,
                    capSize,
                    exploredColor,
                    unexploredColor,
                    adminCreative,
                    mapCache);
            renderConcaveBoundaryCornerCap(
                    guiGraphics,
                    chunkSet,
                    renderedCorners,
                    chunk.x + 1,
                    chunk.z,
                    offsetX,
                    offsetZ,
                    scale,
                    capSize,
                    exploredColor,
                    unexploredColor,
                    adminCreative,
                    mapCache);
            renderConcaveBoundaryCornerCap(
                    guiGraphics,
                    chunkSet,
                    renderedCorners,
                    chunk.x,
                    chunk.z + 1,
                    offsetX,
                    offsetZ,
                    scale,
                    capSize,
                    exploredColor,
                    unexploredColor,
                    adminCreative,
                    mapCache);
            renderConcaveBoundaryCornerCap(
                    guiGraphics,
                    chunkSet,
                    renderedCorners,
                    chunk.x + 1,
                    chunk.z + 1,
                    offsetX,
                    offsetZ,
                    scale,
                    capSize,
                    exploredColor,
                    unexploredColor,
                    adminCreative,
                    mapCache);
        }
    }

    private static void renderConcaveBoundaryCornerCap(
            GuiGraphics guiGraphics,
            LongOpenHashSet chunkSet,
            LongOpenHashSet renderedCorners,
            int gridX,
            int gridZ,
            double offsetX,
            double offsetZ,
            double scale,
            double capSize,
            int exploredColor,
            int unexploredColor,
            boolean adminCreative,
            WorldMapCacheManager mapCache) {
        long cornerKey = gridKey(gridX, gridZ);
        if (!renderedCorners.add(cornerKey)) return;

        boolean nw = hasClaimChunk(chunkSet, gridX - 1, gridZ - 1);
        boolean ne = hasClaimChunk(chunkSet, gridX, gridZ - 1);
        boolean sw = hasClaimChunk(chunkSet, gridX - 1, gridZ);
        boolean se = hasClaimChunk(chunkSet, gridX, gridZ);
        int count = (nw ? 1 : 0) + (ne ? 1 : 0) + (sw ? 1 : 0) + (se ? 1 : 0);
        if (count != 3) return;

        double pixelX = offsetX + gridX * 16.0 * scale;
        double pixelZ = offsetZ + gridZ * 16.0 * scale;
        int color = getConcaveCornerColor(
                chunkSet, gridX, gridZ, exploredColor, unexploredColor, adminCreative, mapCache);

        if (!nw) {
            MapRenderUtil.fill(
                    guiGraphics,
                    pixelX,
                    pixelZ,
                    pixelX + capSize,
                    pixelZ + capSize,
                    color);
        } else if (!ne) {
            MapRenderUtil.fill(
                    guiGraphics,
                    pixelX - capSize,
                    pixelZ,
                    pixelX,
                    pixelZ + capSize,
                    color);
        } else if (!sw) {
            MapRenderUtil.fill(
                    guiGraphics,
                    pixelX,
                    pixelZ - capSize,
                    pixelX + capSize,
                    pixelZ,
                    color);
        } else if (!se) {
            MapRenderUtil.fill(
                    guiGraphics,
                    pixelX - capSize,
                    pixelZ - capSize,
                    pixelX,
                    pixelZ,
                    color);
        }
    }

    private static int getConcaveCornerColor(
            LongOpenHashSet chunkSet,
            int gridX,
            int gridZ,
            int exploredColor,
            int unexploredColor,
            boolean adminCreative,
            WorldMapCacheManager mapCache) {
        if (adminCreative || !ClientManager.configFogOfWarEnabled || mapCache == null) return exploredColor;
        if (isClaimChunkExplored(chunkSet, mapCache, gridX - 1, gridZ - 1)) return exploredColor;
        if (isClaimChunkExplored(chunkSet, mapCache, gridX, gridZ - 1)) return exploredColor;
        if (isClaimChunkExplored(chunkSet, mapCache, gridX - 1, gridZ)) return exploredColor;
        if (isClaimChunkExplored(chunkSet, mapCache, gridX, gridZ)) return exploredColor;
        return unexploredColor;
    }

    private static boolean isClaimChunkExplored(
            LongOpenHashSet chunkSet, WorldMapCacheManager mapCache, int chunkX, int chunkZ) {
        return hasClaimChunk(chunkSet, chunkX, chunkZ) && mapCache.isChunkExplored(new ChunkPos(chunkX, chunkZ));
    }

    private static boolean hasClaimChunk(LongOpenHashSet chunkSet, int chunkX, int chunkZ) {
        return chunkSet.contains(WorldMapClaimIndex.chunkKey(chunkX, chunkZ));
    }

    private static long gridKey(int gridX, int gridZ) {
        return (gridX & 0xFFFFFFFFL) | ((gridZ & 0xFFFFFFFFL) << 32);
    }

    public static void renderClaimName(
            GuiGraphics guiGraphics,
            RecruitsClaim claim,
            double offsetX,
            double offsetZ,
            double scale,
            RenderBounds bounds) {
        if (scale < 1.0) return;

        ClaimShape shape = getShape(claim);
        if (shape == null || !bounds.intersects(shape)) return;

        boolean explored =
                !ClientManager.configFogOfWarEnabled || isAdminCreative() || isClaimExplored(claim);

        Font font = Minecraft.getInstance().font;
        String name = explored ? claim.getName() : "???";
        int nameColor = explored ? 0xFFFFFF : 0x888888;

        double centerWorldX = (shape.minX + shape.maxX + 1) * 16.0 / 2.0;
        double centerWorldZ = (shape.minZ + shape.maxZ + 1) * 16.0 / 2.0;

        double pixelX = offsetX + centerWorldX * scale;
        double pixelZ = offsetZ + centerWorldZ * scale;

        float textScale = (float) Math.min(1.0, scale / 1.25);

        int textWidth = font.width(name);
        int textHeight = font.lineHeight;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        pose.translate(
                pixelX - (textWidth * textScale) / 2.0, pixelZ - (textHeight * textScale) / 2.0, 0);

        pose.scale(textScale, textScale, 1.0f);

        guiGraphics.drawString(font, name, 0, 0, nameColor, false);

        pose.popPose();
    }

    public static int getClaimColor(RecruitsClaim claim) {
        if (claim == null || claim.getOwnerFaction() == null) return 0xFF888888;

        int colorKey = claim.getOwnerFaction().getUnitColor();
        if (colorKey < 0 || colorKey >= FactionEditScreen.unitColors.size()) return 0xFF888888;
        java.awt.Color color = FactionEditScreen.unitColors.get(colorKey);
        return color == null ? 0xFF888888 : color.getRGB();
    }

    public static RecruitsClaim getClaimAtPosition(
            double mouseX, double mouseY, double offsetX, double offsetZ, double scale) {
        double worldX = (mouseX - offsetX) / scale;
        double worldZ = (mouseY - offsetZ) / scale;

        int chunkX = (int) Math.floor(worldX / 16);
        int chunkZ = (int) Math.floor(worldZ / 16);

        return WorldMapClaimIndex.getClaimAt(chunkX, chunkZ);
    }

    public static void renderBufferZone(
            GuiGraphics guiGraphics, double offsetX, double offsetZ, double scale) {
        if (ClientManager.ownFaction == null) return;
        LongOpenHashSet renderedBufferChunks = new LongOpenHashSet();
        int bufferColor = 0x44FF4444;
        RenderBounds bounds = RenderBounds.fromScreen(offsetX, offsetZ, scale);

        LongOpenHashSet ownClaimedChunks = new LongOpenHashSet();
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim != null
                    && claim.getOwnerFaction() != null
                    && claim.getClaimedChunks() != null
                    && claim.getOwnerFaction().getStringID().equals(ClientManager.ownFaction.getStringID())) {
                for (ChunkPos chunk : claim.getClaimedChunks()) {
                    if (chunk != null) ownClaimedChunks.add(WorldMapClaimIndex.chunkKey(chunk));
                }
            }
        }

        for (RecruitsClaim foreignClaim : ClientManager.recruitsClaims) {
            if (foreignClaim == null
                    || foreignClaim.getOwnerFaction() == null
                    || foreignClaim.getClaimedChunks() == null
                    || foreignClaim
                            .getOwnerFaction()
                            .getStringID()
                            .equals(ClientManager.ownFaction.getStringID())) {
                continue;
            }

            for (ChunkPos claimChunk : foreignClaim.getClaimedChunks()) {
                if (claimChunk == null) continue;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {

                        if (dx == 0 && dz == 0) continue;

                        int bufferX = claimChunk.x + dx;
                        int bufferZ = claimChunk.z + dz;
                        if (!bounds.contains(bufferX, bufferZ)) continue;

                        long chunkKey = WorldMapClaimIndex.chunkKey(bufferX, bufferZ);

                        if (renderedBufferChunks.contains(chunkKey)) continue;

                        if (ownClaimedChunks.contains(chunkKey)) continue;

                        renderedBufferChunks.add(chunkKey);

                        ChunkPos bufferChunk = new ChunkPos(bufferX, bufferZ);
                        renderChunk(guiGraphics, bufferChunk, bufferColor, offsetX, offsetZ, scale, bounds);
                    }
                }
            }
        }
    }

    public static void renderClaimPreview(
            GuiGraphics guiGraphics,
            List<ClaimPreviewChunk> previewChunks,
            double offsetX,
            double offsetZ,
            double scale) {
        if (previewChunks == null || previewChunks.isEmpty()) return;

        RenderBounds bounds = RenderBounds.fromScreen(offsetX, offsetZ, scale);
        for (ClaimPreviewChunk preview : previewChunks) {
            if (preview == null || preview.chunk() == null || preview.status() == null) continue;
            renderChunk(guiGraphics, preview.chunk(), preview.status().previewColor(), offsetX, offsetZ, scale, bounds);
        }
    }

    private static ClaimShape getShape(RecruitsClaim claim) {
        if (claim == null || claim.getClaimedChunks() == null || claim.getClaimedChunks().isEmpty()) return null;

        UUID claimId = claim.getUUID();
        if (claimId != null) {
            ClaimShape cached = CLAIM_SHAPES.get(claimId);
            if (cached != null && cached.claim == claim) return cached;
        }

        ClaimShape shape = ClaimShape.build(claim);
        if (shape != null && claimId != null) {
            CLAIM_SHAPES.put(claimId, shape);
        }
        return shape;
    }

    private static final class ClaimShape {
        private final RecruitsClaim claim;
        private final LongOpenHashSet chunkSet;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private ClaimShape(
                RecruitsClaim claim, LongOpenHashSet chunkSet, int minX, int maxX, int minZ, int maxZ) {
            this.claim = claim;
            this.chunkSet = chunkSet;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        private static ClaimShape build(RecruitsClaim claim) {
            List<ChunkPos> chunks = claim.getClaimedChunks();
            LongOpenHashSet chunkSet = new LongOpenHashSet(chunks.size());
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (ChunkPos chunk : chunks) {
                if (chunk == null) continue;
                chunkSet.add(WorldMapClaimIndex.chunkKey(chunk));
                minX = Math.min(minX, chunk.x);
                maxX = Math.max(maxX, chunk.x);
                minZ = Math.min(minZ, chunk.z);
                maxZ = Math.max(maxZ, chunk.z);
            }

            if (minX == Integer.MAX_VALUE) return null;
            return new ClaimShape(claim, chunkSet, minX, maxX, minZ, maxZ);
        }
    }

    private record RenderBounds(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        private static RenderBounds fromScreen(double offsetX, double offsetZ, double scale) {
            if (scale <= 0.0) {
                return new RenderBounds(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            }

            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            double margin = Math.max(256.0, Math.max(width, height));

            int minChunkX = (int) Math.floor(((-margin - offsetX) / scale) / 16.0) - 1;
            int maxChunkX = (int) Math.floor(((width + margin - offsetX) / scale) / 16.0) + 1;
            int minChunkZ = (int) Math.floor(((-margin - offsetZ) / scale) / 16.0) - 1;
            int maxChunkZ = (int) Math.floor(((height + margin - offsetZ) / scale) / 16.0) + 1;
            return new RenderBounds(minChunkX, maxChunkX, minChunkZ, maxChunkZ);
        }

        private boolean intersects(ClaimShape shape) {
            return shape.maxX >= minChunkX
                    && shape.minX <= maxChunkX
                    && shape.maxZ >= minChunkZ
                    && shape.minZ <= maxChunkZ;
        }

        private boolean contains(ChunkPos chunk) {
            return contains(chunk.x, chunk.z);
        }

        private boolean contains(int chunkX, int chunkZ) {
            return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
        }

        private boolean isNear(ChunkPos chunk, int marginChunks) {
            return chunk.x >= minChunkX - marginChunks
                    && chunk.x <= maxChunkX + marginChunks
                    && chunk.z >= minChunkZ - marginChunks
                    && chunk.z <= maxChunkZ + marginChunks;
        }
    }

}
