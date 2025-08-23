package com.talhanation.recruits.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
public class FactionClaimBannerOverlay {
    private static final long FULL_DISPLAY_DURATION = 5000;
    private static final long ANIMATION_TIME = 1000;

    private static long startTime = -1;
    private static long stripStartTime = -1;
    private static boolean active = false;

    private static RecruitsTeam currentTeam;
    private static String claimName;
    private static String playerName;
    private static BannerRenderer bannerRenderer;

    public static void activate(RecruitsClaim claim) {
        currentTeam = claim.getOwnerFaction();
        FactionClaimBannerOverlay.claimName = claim.getName();
        playerName = claim.getPlayerInfo().getName();
        bannerRenderer = new BannerRenderer(claim.getOwnerFaction());
        startTime = System.currentTimeMillis();
        stripStartTime = -1;
        active = true;
    }

    public static void deactivate() {
        active = false;
        stripStartTime = System.currentTimeMillis();
    }

    public static void renderOverlay(GuiGraphics guiGraphics, int screenWidth) {
        if (startTime <= 0 || currentTeam == null) return;

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed < FULL_DISPLAY_DURATION + ANIMATION_TIME) {
            int yOffset;
            if (elapsed < ANIMATION_TIME) {

                float t = elapsed / (float) ANIMATION_TIME;
                yOffset = (int) (-50 + 50 * t);
            } else if (elapsed < FULL_DISPLAY_DURATION) {
                yOffset = 0;
            } else {
                // Ausblenden Banner
                float t = (elapsed - FULL_DISPLAY_DURATION) / (float) ANIMATION_TIME;
                yOffset = (int) (-50 * t);

                if (t >= 1f && stripStartTime < 0) {
                    stripStartTime = System.currentTimeMillis();
                }
            }

            drawBanner(guiGraphics, screenWidth, yOffset);
            return;
        }

        if (stripStartTime < 0) {
            stripStartTime = now;
        }

        long stripElapsed = now - stripStartTime;
        int yOffset = 0;

        if (active) {
            if (stripElapsed < ANIMATION_TIME) {
                float t = stripElapsed / (float) ANIMATION_TIME;
                yOffset = (int) (-20 + 20 * t);
            }
        } else {
            if (stripElapsed < ANIMATION_TIME) {
                float t = stripElapsed / (float) ANIMATION_TIME;
                yOffset = (int) (-20 * t);
            } else {
                reset();
                return;
            }
        }

        drawStrip(guiGraphics, screenWidth, yOffset);
    }

    private static void drawBanner(GuiGraphics guiGraphics, int screenWidth, int yOffset) {
        int width = 150;
        int height = 50;
        int x = (screenWidth - width) / 2;
        int y = 10 + yOffset;

        // Hintergrund
        int alpha = 5;
        int rgb = TeamEditScreen.unitColors.get(currentTeam.getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(x - 5, y - 5, x + width + 5, y + height + 5, argb);
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;
        bannerRenderer.renderBanner(guiGraphics, x + 5, y + 17, width, height, 25);
        guiGraphics.drawString(font, Component.literal(claimName), x + 40, y + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.literal(currentTeam.getTeamDisplayName()), x + 40, y + 20, 0xAAAAAA);
        guiGraphics.drawString(font, Component.literal(playerName), x + 40, y + 35, 0xAAAAAA);
    }

    private static void drawStrip(GuiGraphics guiGraphics, int screenWidth, int yOffset) {
        int width = 150;
        int height = 10;
        int x = (screenWidth - width) / 2;
        int y = 10 + yOffset;

        int alpha = 5;
        int rgb = TeamEditScreen.unitColors.get(currentTeam.getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(x - 5, y - 5, x + width + 5, y + height + 5, argb);
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;
        String text = claimName + " - " + currentTeam.getTeamDisplayName();
        guiGraphics.drawCenteredString(font, text, screenWidth / 2, y + 1, 0xFFFFFF);
    }

    private static void reset() {
        startTime = -1;
        stripStartTime = -1;
        currentTeam = null;
        claimName = null;
        playerName = null;
        bannerRenderer = null;
    }

    public static void update(RecruitsClaim claim) {
        claimName = claim.getName();
        playerName = claim.getPlayerInfo().getName();
        currentTeam = claim.getOwnerFaction();
    }
}

