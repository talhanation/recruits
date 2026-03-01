package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.faction.FactionEditScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClaimRenderer {

    public static void renderClaimsOverlay(GuiGraphics guiGraphics, RecruitsClaim selectedClaim, double offsetX, double offsetZ, double scale) {
        if (ClientManager.recruitsClaims.isEmpty()) return;

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimFill(guiGraphics, claim, offsetX, offsetZ, scale);
        }

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimPassiveOutline(guiGraphics, claim, offsetX, offsetZ, scale);
        }

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            renderClaimName(guiGraphics, claim, offsetX, offsetZ, scale);
        }

        if (selectedClaim != null) {
            renderClaimSelectedOutline(guiGraphics, selectedClaim, offsetX, offsetZ, scale);
        }
    }

    private static void renderClaimFill(GuiGraphics guiGraphics, RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
        if (claim.getClaimedChunks().isEmpty()) return;

        int color = getClaimColor(claim);
        int fillColor = (190 << 24) | (color & 0x00FFFFFF);

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            renderChunk(guiGraphics, chunk, fillColor, offsetX, offsetZ, scale);
        }
    }

    private static void renderChunk(GuiGraphics guiGraphics, ChunkPos chunk, int color, double offsetX, double offsetZ, double scale) {
        double worldX = chunk.x * 16.0;
        double worldZ = chunk.z * 16.0;

        int x1 = (int) Math.floor(offsetX + worldX * scale);
        int z1 = (int) Math.floor(offsetZ + worldZ * scale);

        int x2 = (int) Math.floor(offsetX + (worldX + 16.0) * scale);
        int z2 = (int) Math.floor(offsetZ + (worldZ + 16.0) * scale);

        if (x2 <= x1) x2 = x1 + 1;
        if (z2 <= z1) z2 = z1 + 1;

        guiGraphics.fill(x1, z1, x2, z2, color);
    }

    private static void renderClaimPassiveOutline(GuiGraphics guiGraphics, RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
        if (claim.getClaimedChunks().isEmpty()) return;

        Set<ChunkPos> chunkSet = new HashSet<>(claim.getClaimedChunks());

        int baseColor = getClaimColor(claim);
        int outlineColor = (200 << 24) | (baseColor & 0x00FFFFFF);

        int thickness = Math.max(1, (int)Math.round(scale * 0.5));

        for (ChunkPos chunk : claim.getClaimedChunks()) {

            boolean hasTop    = chunkSet.contains(new ChunkPos(chunk.x, chunk.z - 1));
            boolean hasBottom = chunkSet.contains(new ChunkPos(chunk.x, chunk.z + 1));
            boolean hasLeft   = chunkSet.contains(new ChunkPos(chunk.x - 1, chunk.z));
            boolean hasRight  = chunkSet.contains(new ChunkPos(chunk.x + 1, chunk.z));

            double worldX1 = chunk.x * 16.0;
            double worldZ1 = chunk.z * 16.0;

            int x1 = (int)Math.floor(offsetX + worldX1 * scale);
            int z1 = (int)Math.floor(offsetZ + worldZ1 * scale);
            int x2 = (int)Math.floor(offsetX + (worldX1 + 16.0) * scale);
            int z2 = (int)Math.floor(offsetZ + (worldZ1 + 16.0) * scale);

            if (x2 <= x1) x2 = x1 + 1;
            if (z2 <= z1) z2 = z1 + 1;

            if (!hasTop) {
                guiGraphics.fill(x1, z1, x2, z1 + thickness, outlineColor);
            }
            if (!hasBottom) {
                guiGraphics.fill(x1, z2 - thickness, x2, z2, outlineColor);
            }
            if (!hasLeft) {
                guiGraphics.fill(x1, z1, x1 + thickness, z2, outlineColor);
            }
            if (!hasRight) {
                guiGraphics.fill(x2 - thickness, z1, x2, z2, outlineColor);
            }
        }
    }

    private static void renderClaimSelectedOutline(GuiGraphics guiGraphics, RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
        if (claim.getClaimedChunks().isEmpty()) return;

        Set<String> chunkSet = new HashSet<>();
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            chunkSet.add(chunk.x + "," + chunk.z);
        }

        int borderColor = 0xFFFFFFFF;
        int borderThickness = Math.max(1, (int)(2 * scale / 2.0));

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            boolean hasTop = chunkSet.contains(chunk.x + "," + (chunk.z - 1));
            boolean hasBottom = chunkSet.contains(chunk.x + "," + (chunk.z + 1));
            boolean hasLeft = chunkSet.contains((chunk.x - 1) + "," + chunk.z);
            boolean hasRight = chunkSet.contains((chunk.x + 1) + "," + chunk.z);

            if (hasTop && hasBottom && hasLeft && hasRight) {
                continue;
            }

            double worldX1 = chunk.x * 16.0;
            double worldZ1 = chunk.z * 16.0;
            double worldX2 = worldX1 + 16.0;
            double worldZ2 = worldZ1 + 16.0;

            int x1 = (int) Math.floor(offsetX + worldX1 * scale);
            int z1 = (int) Math.floor(offsetZ + worldZ1 * scale);
            int x2 = (int) Math.floor(offsetX + worldX2 * scale);
            int z2 = (int) Math.floor(offsetZ + worldZ2 * scale);

            if (!hasTop) {
                guiGraphics.fill(x1, z1, x2, z1 + borderThickness, borderColor);
            }
            if (!hasBottom) {
                guiGraphics.fill(x1, z2 - borderThickness, x2, z2, borderColor);
            }
            if (!hasLeft) {
                guiGraphics.fill(x1, z1, x1 + borderThickness, z2, borderColor);
            }
            if (!hasRight) {
                guiGraphics.fill(x2 - borderThickness, z1, x2, z2, borderColor);
            }
        }
    }

    public static void renderClaimName(GuiGraphics guiGraphics, RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
        if (claim.getClaimedChunks().isEmpty() || scale < 1.0) return;

        Font font = Minecraft.getInstance().font;
        String name = claim.getName();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (ChunkPos pos : claim.getClaimedChunks()) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minZ = Math.min(minZ, pos.z);
            maxZ = Math.max(maxZ, pos.z);
        }

        double centerWorldX = (minX + maxX + 1) * 16.0 / 2.0;
        double centerWorldZ = (minZ + maxZ + 1) * 16.0 / 2.0;

        double pixelX = offsetX + centerWorldX * scale;
        double pixelZ = offsetZ + centerWorldZ * scale;

        float textScale = (float)Math.min(1.0, scale / 1.25);

        int textWidth = font.width(name);
        int textHeight = font.lineHeight;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        pose.translate(pixelX - (textWidth * textScale) / 2.0, pixelZ - (textHeight * textScale) / 2.0, 0);

        pose.scale(textScale, textScale, 1.0f);

        guiGraphics.drawString(font, name, 0, 0, 0xFFFFFF, false);

        pose.popPose();
    }

    public static int getClaimColor(RecruitsClaim claim) {
        if (claim.getOwnerFaction() == null) return 0xFF888888;

        byte colorKey = claim.getOwnerFaction().getUnitColor();
        return FactionEditScreen.unitColors.get(colorKey).getRGB();
    }

    public static RecruitsClaim getClaimAtPosition(double mouseX, double mouseY, double offsetX, double offsetZ, double scale) {
        double worldX = (mouseX - offsetX) / scale;
        double worldZ = (mouseY - offsetZ) / scale;

        int chunkX = (int)Math.floor(worldX / 16);
        int chunkZ = (int)Math.floor(worldZ / 16);
        ChunkPos mouseChunk = new ChunkPos(chunkX, chunkZ);

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.containsChunk(mouseChunk)) {
                return claim;
            }
        }

        return null;
    }

    public static void renderBufferZone(GuiGraphics guiGraphics, double offsetX, double offsetZ, double scale) {
        if (ClientManager.ownFaction == null) return;
        Set<String> renderedBufferChunks = new HashSet<>();
        int bufferColor = 0x44FF4444;

        Set<String> ownClaimedChunks = new HashSet<>();
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.getOwnerFaction() != null && claim.getOwnerFaction().getStringID().equals(ClientManager.ownFaction.getStringID())) {
                for (ChunkPos chunk : claim.getClaimedChunks()) {
                    ownClaimedChunks.add(chunk.x + "," + chunk.z);
                }
            }
        }

        for (RecruitsClaim foreignClaim : ClientManager.recruitsClaims) {
            if (foreignClaim.getOwnerFaction() == null || foreignClaim.getOwnerFaction().getStringID().equals(ClientManager.ownFaction.getStringID())) {
                continue;
            }

            for (ChunkPos claimChunk : foreignClaim.getClaimedChunks()) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {

                        if (dx == 0 && dz == 0) continue;

                        int bufferX = claimChunk.x + dx;
                        int bufferZ = claimChunk.z + dz;
                        String chunkKey = bufferX + "," + bufferZ;

                        if (renderedBufferChunks.contains(chunkKey)) continue;

                        if (ownClaimedChunks.contains(chunkKey)) continue;

                        renderedBufferChunks.add(chunkKey);

                        ChunkPos bufferChunk = new ChunkPos(bufferX, bufferZ);
                        renderChunk(guiGraphics, bufferChunk, bufferColor, offsetX, offsetZ, scale);
                    }
                }
            }
        }
    }

    public static void renderAreaPreview(GuiGraphics guiGraphics, List<ChunkPos> areaChunks, double offsetX, double offsetZ, double scale) {
        if (areaChunks == null || areaChunks.isEmpty()) return;

        int previewColor = 0x33FFFFFF;

        for (ChunkPos chunk : areaChunks) {
            renderChunk(guiGraphics, chunk, previewColor, offsetX, offsetZ, scale);
        }
    }


}
