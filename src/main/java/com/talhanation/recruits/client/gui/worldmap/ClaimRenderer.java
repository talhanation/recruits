package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.faction.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
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
        if (claim.getCenter() == null || scale < 1.0) return;

        ChunkPos center = claim.getCenter();
        double pixelX = offsetX + center.x * 16 * scale;
        double pixelZ = offsetZ + center.z * 16 * scale;
        double size = 64;

        String name = claim.getName();
        Font font = Minecraft.getInstance().font;

        float textScale = Math.min(1.0f, (float)(size / 100.0));
        int textWidth = (int)(font.width(name) * textScale);

        int x = (int)(pixelX + (size - textWidth) / 2);
        int y = (int)(pixelZ + (size - 9 * textScale) / 2);

        int padding = 2;
        /*guiGraphics.fill(
                x - padding, y - padding,
                x + textWidth + padding, y + (int)(9 * textScale) + padding,
                0x80000000
        );*/

        // Text
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(textScale, textScale, 1.0f);
        guiGraphics.drawString(font, name, 0, 0, 0xFFFFFF);
        pose.popPose();
    }

    public static int getClaimColor(RecruitsClaim claim) {
        if (claim.getOwnerFaction() == null) return 0xFF888888;

        byte colorKey = claim.getOwnerFaction().getUnitColor();
        return TeamEditScreen.unitColors.get(colorKey).getRGB();
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
}
