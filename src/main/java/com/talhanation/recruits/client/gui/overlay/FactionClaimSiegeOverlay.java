package com.talhanation.recruits.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.faction.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FactionClaimSiegeOverlay {
    private static final ResourceLocation CENTER_ICON = new ResourceLocation("recruits:textures/gui/image/enemy.png");
    private static final int ANIMATION_TIME = 1000;
    private static String claimName;
    private static RecruitsFaction defenderTeam;
    private static RecruitsFaction attackerTeam;
    private static float currentHealth;
    private static float maxHealth;
    private static BannerRenderer defenderBanner;
    private static BannerRenderer attackerBanner;

    private static boolean active = false;
    private static boolean animatingOut = false;
    private static long animationStart = -1;

    public static void activate(String claimName, RecruitsFaction defender, RecruitsFaction attacker, float currentHealth, float maxHealth) {
        FactionClaimSiegeOverlay.claimName = claimName;
        FactionClaimSiegeOverlay.defenderTeam = defender;
        FactionClaimSiegeOverlay.attackerTeam = attacker;
        FactionClaimSiegeOverlay.currentHealth = currentHealth;
        FactionClaimSiegeOverlay.maxHealth = maxHealth;
        FactionClaimSiegeOverlay.defenderBanner = new BannerRenderer(defender);
        FactionClaimSiegeOverlay.attackerBanner = new BannerRenderer(attacker);

        active = true;
        animatingOut = false;
        animationStart = System.currentTimeMillis();
    }

    public static void deactivate() {
        if (!active || animatingOut) return;
        animatingOut = true;
        animationStart = System.currentTimeMillis();
    }

    public static void renderOverlay(GuiGraphics guiGraphics, int screenWidth) {
        if (!active && !animatingOut) return;

        long now = System.currentTimeMillis();
        float progress = Math.min(1f, (now - animationStart) / (float) ANIMATION_TIME);

        int yOffset;
        if (!animatingOut) {
            if (progress < 1f) {
                yOffset = (int) (-60 + 60 * progress);
            } else {
                yOffset = 0;
            }
        } else {

            if (progress < 1f) {
                yOffset = (int) (-60 * progress);
            } else {
                reset();
                return;
            }
        }

        drawSiegePanel(guiGraphics, screenWidth, yOffset);
    }

    private static void drawSiegePanel(GuiGraphics guiGraphics, int screenWidth, int yOffset) {
        int panelWidth = 190;
        int panelHeight = 40;
        int x = (screenWidth - panelWidth) / 2;
        int y = 10 + yOffset;

        int alpha = 7;
        int rgb = TeamEditScreen.unitColors.get(defenderTeam.getUnitColor()).getRGB();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(x - 4, y - 4, x + panelWidth + 4, y + panelHeight + 8, argb);
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;

        guiGraphics.drawCenteredString(font, Component.literal(claimName), x + panelWidth / 2, y + 1, 0xFFFFFF);

        if(defenderTeam != null && defenderBanner != null){
            defenderBanner.renderBanner(guiGraphics, x + 5, y + 18, 20, 20, 20);
            guiGraphics.drawCenteredString(font, Component.literal(defenderTeam.getTeamDisplayName()), x + 15, y + 1, 0xAAAAAA);

        }
        if(attackerTeam != null){
            attackerBanner.renderBanner(guiGraphics, x + panelWidth - 25, y + 18, 20, 20, 20);
            guiGraphics.drawCenteredString(font, Component.literal(attackerTeam.getTeamDisplayName()), x + panelWidth - 15, y + 1, 0xAAAAAA);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, CENTER_ICON);
        int iconSize = 18;
        guiGraphics.blit(CENTER_ICON, x + panelWidth / 2 - iconSize / 2, y + 14, 0, 0, iconSize, iconSize, iconSize, iconSize);

        int barWidth = 100;
        int barHeight = 4;
        int barX = x + panelWidth / 2 - barWidth / 2;
        int barY = y + 34;
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);

        if (maxHealth > 0) {
            float ratio = currentHealth / maxHealth;
            guiGraphics.fill(barX, barY, barX + (int) (barWidth * ratio), barY + barHeight, 0xFF00FF00);
        }
    }

    private static void reset() {
        active = false;
        animatingOut = false;
        animationStart = -1;
        claimName = null;
        defenderTeam = null;
        attackerTeam = null;
        defenderBanner = null;
        attackerBanner = null;
    }

    public static void update(RecruitsClaim claim) {
        currentHealth = claim.getHealth();
        maxHealth = claim.getMaxHealth();
        attackerTeam = claim.attackingParties != null && !claim.attackingParties.isEmpty() ? claim.attackingParties.get(0) : null;
        defenderTeam = claim.getOwnerFaction();
        FactionClaimSiegeOverlay.defenderBanner = new BannerRenderer(defenderTeam);
        FactionClaimSiegeOverlay.attackerBanner = new BannerRenderer(attackerTeam);
    }
}

