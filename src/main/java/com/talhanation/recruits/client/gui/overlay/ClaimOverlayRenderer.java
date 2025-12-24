package com.talhanation.recruits.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.faction.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ClaimOverlayRenderer {
    private final Map<RecruitsFaction, BannerRenderer> bannerCache = new HashMap<>();
    private final Map<RecruitsFaction, Integer> factionColorCache = new HashMap<>();

    private static final int PANEL_HEIGHT_FULL = 45;
    private static final int PANEL_HEIGHT_COMPACT = 15;
    private static final int BACKGROUND_ALPHA = 0x0F;
    private static final ResourceLocation SIEGE_ICON = new ResourceLocation("recruits:textures/gui/image/enemy.png");

    private boolean dataChanged = true;

    public void render(GuiGraphics guiGraphics, Minecraft minecraft, RecruitsClaim claim, ClaimOverlayManager.OverlayState state, float alpha, int panelWidth) {
        if (claim == null || state == ClaimOverlayManager.OverlayState.HIDDEN) return;

        Font font = minecraft.font;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = (screenWidth - panelWidth) / 2;
        int y = 10;
        int panelHeight = (state == ClaimOverlayManager.OverlayState.FULL) ? PANEL_HEIGHT_FULL : PANEL_HEIGHT_COMPACT;

        int bgAlpha = (int)(BACKGROUND_ALPHA * alpha);

        int factionColor = getFactionColor(claim.getOwnerFaction());
        int backgroundColor = (bgAlpha << 24) | (factionColor & 0x00FFFFFF);

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, backgroundColor);

        if (claim.isUnderSiege) {
            renderSiegeContent(guiGraphics, claim, x, y, panelWidth, panelHeight, font, alpha);
        } else {
            if (state == ClaimOverlayManager.OverlayState.FULL) {
                renderNormalFullContent(guiGraphics, claim, x, y, panelWidth, panelHeight, font, alpha);
            } else {
                renderNormalCompactContent(guiGraphics, claim, x, y, panelWidth, panelHeight, font, alpha);
            }
        }

        dataChanged = false;
    }

    private void renderNormalFullContent(GuiGraphics guiGraphics, RecruitsClaim claim, int x, int y, int width, int height, Font font, float alpha) {
        RecruitsFaction faction = claim.getOwnerFaction();

        int textAlpha = (int)(0xFF * alpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        int bannerX = 5;
        int bannerY = 13;
        BannerRenderer banner = getBannerRenderer(faction);
        if (banner != null) {
            banner.renderBanner(guiGraphics, x + bannerX, y + bannerY, 48, 48, 20);
        }

        String claimName = truncateText(font, claim.getName(), width - 80);
        guiGraphics.drawString(font, claimName, x + 60, y + 10, textColor, false);


        if (claim.getPlayerInfo() != null) {
            String claimOwner = truncateText(font, claim.getPlayerInfo().getName(), width - 80);

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            float scale = 0.5f;

            int originalX = x + 60;
            int originalY = y + 20;

            poseStack.translate(originalX, originalY, 0);
            poseStack.scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, faction.getTeamDisplayName(), 0, 0, 0xAAAAAA, false);
            guiGraphics.drawString(font, claimOwner, 0, 10, 0xAAAAAA, false);

            poseStack.popPose();
        }
    }

    private void renderNormalCompactContent(GuiGraphics guiGraphics, RecruitsClaim claim, int x, int y, int width, int height, Font font, float alpha) {
        int textAlpha = (int)(0xFF * alpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        String displayText = truncateText(font, claim.getName() + " - " + claim.getOwnerFaction().getTeamDisplayName(), width - 20);

        int textWidth = font.width(displayText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 9) / 2;

        guiGraphics.drawString(font, displayText, textX, textY, textColor, false);
    }

    private void renderSiegeContent(GuiGraphics guiGraphics, RecruitsClaim claim, int x, int y, int width, int height, Font font, float alpha) {
        int textAlpha = (int)(0xFF * alpha);
        int normalTextColor = (textAlpha << 24) | 0xFFFFFF;

        String name = truncateText(font, claim.getName(), width - 40);
        int nameWidth = font.width(name);
        int nameX = x + (width - nameWidth) / 2;
        guiGraphics.drawString(font, name, nameX, y + 2, normalTextColor, false);

        int bannerSize = 48;
        int bannerY = y + 15;

        BannerRenderer ownerBanner = getBannerRenderer(claim.getOwnerFaction());
        if (ownerBanner != null) {
            ownerBanner.renderBanner(guiGraphics, x + 5, bannerY, bannerSize, bannerSize, 15);
        }

        String ownerFactionName = truncateText(font, claim.getOwnerFaction().getTeamDisplayName(), 60);
        int ownerFactionNameWidth = font.width(ownerFactionName);
        guiGraphics.drawString(font, ownerFactionName, x + 15 - ownerFactionNameWidth/2, y + 2, 0xAAAAAA, false);

        if (!claim.attackingParties.isEmpty()) {
            BannerRenderer attackerBanner = getBannerRenderer(claim.attackingParties.get(0));
            if (attackerBanner != null) {
                attackerBanner.renderBanner(guiGraphics, x - 25 + width, bannerY, bannerSize, bannerSize, 15);
            }
            String attackerName = truncateText(font, claim.attackingParties.get(0).getTeamDisplayName(), 60);
            int attackerNameWidth = font.width(name);
            int attackerNameX = x - 15 + width - attackerNameWidth/2;

            guiGraphics.drawString(font, attackerName, attackerNameX, y + 2, 0xAAAAAA, false);
        }

        int barWidth = 100;
        int barHeight = 4;
        int barX = x + width / 2 - barWidth / 2;
        int barY = y + 35;
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);

        if (claim.getMaxHealth() > 0) {
            float ratio = (float) claim.getHealth() / claim.getMaxHealth();
            guiGraphics.fill(barX, barY, barX + (int) (barWidth * ratio), barY + barHeight, 0xFF00FF00);
        }


        int iconSize = 18;
        guiGraphics.blit(SIEGE_ICON, x + width / 2 - iconSize / 2, y + 14, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }

    private String truncateText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        while (font.width(text + "...") > maxWidth && text.length() > 3) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private BannerRenderer getBannerRenderer(RecruitsFaction faction) {
        if (faction == null) return null;

        if (dataChanged) {
            bannerCache.remove(faction);
        }

        return bannerCache.computeIfAbsent(faction, BannerRenderer::new);
    }

    private int getFactionColor(RecruitsFaction faction) {
        if (faction == null) return 0x808080;

        return factionColorCache.computeIfAbsent(faction, f -> {
            int alpha = 7;
            int rgb = TeamEditScreen.unitColors.get(faction.getUnitColor()).getRGB();
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return (alpha << 24) | (r << 16) | (g << 8) | b;
        });
    }

    private int getHealthColor(float percent) {
        if (percent > 0.6f) return 0x8000FF00;
        if (percent > 0.3f) return 0x80FFFF00;
        return 0x80FF0000;
    }

    public void markDataChanged() {
        dataChanged = true;
    }

    public void clearCache() {
        bannerCache.clear();
        factionColorCache.clear();
        dataChanged = true;
    }
}