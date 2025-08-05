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
import net.minecraft.resources.ResourceLocation;

public class FactionClaimSiegeOverlay {
    private static final int DISPLAY_DURATION = 100000;
    private static final int ANIMATION_TIME = 5000;
    private static final ResourceLocation CENTER_ICON = new ResourceLocation("recruits:textures/gui/image/enemy.png");

    private static int timer = 0;
    private static String claimName;
    private static RecruitsTeam defenderTeam;
    private static RecruitsTeam attackerTeam;
    private static float claimHealth; // 0.0 - 1.0
    private static BannerRenderer defenderBanner;
    private static BannerRenderer attackerBanner;

    public static void display(String claimName, RecruitsTeam defender, RecruitsTeam attacker, float healthRatio) {
        FactionClaimSiegeOverlay.claimName = claimName;
        FactionClaimSiegeOverlay.defenderTeam = defender;
        FactionClaimSiegeOverlay.attackerTeam = attacker;
        FactionClaimSiegeOverlay.claimHealth = healthRatio;
        FactionClaimSiegeOverlay.defenderBanner = new BannerRenderer(defender);
        FactionClaimSiegeOverlay.attackerBanner = new BannerRenderer(attacker);
        FactionClaimSiegeOverlay.timer = DISPLAY_DURATION + ANIMATION_TIME * 2;
    }

    public static void renderOverlay(GuiGraphics guiGraphics, int screenWidth) {
        if (timer <= 0 || defenderTeam == null || attackerTeam == null) return;

        int totalTime = DISPLAY_DURATION + ANIMATION_TIME * 2;
        int elapsed = totalTime - timer;

        int yOffset;
        if (elapsed < ANIMATION_TIME) {
            float t = elapsed / (float) ANIMATION_TIME;
            yOffset = (int) (-60 + 60 * t);
        } else if (elapsed < ANIMATION_TIME + DISPLAY_DURATION) {
            yOffset = 0;
        } else {
            float t = (elapsed - ANIMATION_TIME - DISPLAY_DURATION) / (float) ANIMATION_TIME;
            yOffset = (int) (-60 * t);
        }

        int panelWidth = 250;
        int panelHeight = 70;
        int x = (screenWidth - panelWidth) / 2;
        int y = 10 + yOffset;

        // Hintergrundfarbe vom Defender
        int alpha = 3;
        int rgb = TeamEditScreen.unitColors.get(defenderTeam.getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        guiGraphics.fill(x - 5, y - 5, x + panelWidth + 5, y + panelHeight + 5, argb);
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;

        // Claim Name zentriert oben
        guiGraphics.drawCenteredString(font, Component.literal(claimName), x + panelWidth / 2, y + 5, 0xFFFFFF);

        // Defender Banner + Name links
        defenderBanner.renderBanner(guiGraphics, x + 5, y + 25, 25, 25, 25);
        guiGraphics.drawCenteredString(font, Component.literal(defenderTeam.getTeamDisplayName()), x + 17, y + 15, 0xAAAAAA);

        // Attacker Banner + Name rechts
        attackerBanner.renderBanner(guiGraphics, x + panelWidth - 30, y + 25, 25, 25, 25);
        guiGraphics.drawCenteredString(font, Component.literal(attackerTeam.getTeamDisplayName()), x + panelWidth - 17, y + 15, 0xAAAAAA);

        // Center Icon
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, CENTER_ICON);
        guiGraphics.blit(CENTER_ICON, x + panelWidth / 2 - 10, y + 27, 0, 0, 21, 21, 21, 21);

        // Health Bar unter dem Icon
        int barX = x + panelWidth / 2 - 75;
        int barY = y + 52;
        guiGraphics.fill(barX, barY, barX + 150, barY + 5, 0xFF000000); // Hintergrund
        guiGraphics.fill(barX, barY, barX + (int)(150 * claimHealth), barY + 5, 0xFF00FF00); // Grün je nach Health

        timer--;
    }
}

