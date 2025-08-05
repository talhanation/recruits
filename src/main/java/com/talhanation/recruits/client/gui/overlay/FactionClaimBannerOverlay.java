package com.talhanation.recruits.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public class FactionClaimBannerOverlay {
    private static final long DISPLAY_DURATION = 4000;
    private static final long ANIMATION_TIME = 1000;

    private static long startTime = -1;

    private static RecruitsTeam currentTeam;
    private static String claimName;
    private static BannerRenderer bannerRenderer;

    public static void display(RecruitsTeam team, String claimName) {
        FactionClaimBannerOverlay.currentTeam = team;
        FactionClaimBannerOverlay.claimName = claimName;
        FactionClaimBannerOverlay.bannerRenderer = new BannerRenderer(team);
        FactionClaimBannerOverlay.startTime = System.currentTimeMillis();
    }

    public static void renderOverlay(GuiGraphics guiGraphics, int screenWidth) {
        if (startTime <= 0 || currentTeam == null) return;

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        long totalTime = DISPLAY_DURATION + ANIMATION_TIME * 2;

        if (elapsed >= totalTime) {
            startTime = -1;
            return;
        }

        int yOffset;
        if (elapsed < ANIMATION_TIME) {
            // Einblenden
            float t = elapsed / (float) ANIMATION_TIME;
            yOffset = (int) (-50 + 50 * t);
        } else if (elapsed < ANIMATION_TIME + DISPLAY_DURATION) {
            yOffset = 0;
        } else {
            // Ausblenden
            float t = (elapsed - ANIMATION_TIME - DISPLAY_DURATION) / (float) ANIMATION_TIME;
            yOffset = (int) (-50 * t);
        }

        int width = 150;
        int height = 50;
        int x = (screenWidth - width) / 2;
        int y = 10 + yOffset;

        // Hintergrund
        int alpha = 3;
        int rgb = TeamEditScreen.unitColors.get(currentTeam.getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        guiGraphics.fill(x - 5, y - 5, x + width + 5, y + height + 5, argb);

        RenderSystem.disableBlend();

        // Banner rendern
        bannerRenderer.renderBanner(guiGraphics, x + 5, y + 17, width, height, 25);

        // Claim- & Factionname
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.literal(claimName), x + 40, y + 5, 0xFFFFFF);
        guiGraphics.drawString(font, Component.literal(currentTeam.getTeamDisplayName()), x + 40, y + 20, 0xAAAAAA);
    }
}
