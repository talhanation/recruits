package com.talhanation.recruits.client.gui.worldmap.claim;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class ClaimInfoMenu {
    private static final ResourceLocation SIEGE_ICON = ResourceLocation.parse("recruits:textures/gui/image/enemy.png");
    private static final int PANEL_BACKGROUND = 0xD0000000;
    private static final int PANEL_OUTLINE = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFB8B8B8;
    private static final int VALUE_COLOR = 0xFFFFFFFF;
    private static final int ALLOWED_COLOR = 0xFF55FF77;
    private static final int DENIED_COLOR = 0xFFFF7777;
    private static final int WARNING_COLOR = 0xFFFFAA44;
    private static final int SAFE_BOTTOM_MARGIN = 42;
    private static final int SIDE_BANNER_WIDTH = 58;
    private static final int LABEL_COLUMN_WIDTH = 76;

    private final WorldMapScreen parent;
    private boolean visible = false;
    private RecruitsClaim currentClaim;
    private BannerRenderer bannerRenderer;
    private BannerRenderer bannerRendererAttacker;
    public int x, y;
    public int width = 180, height = 178;

    public ClaimInfoMenu(WorldMapScreen parent) {
        this.parent = parent;
    }

    public void init() {
        this.bannerRenderer = new BannerRenderer(null);
        this.bannerRendererAttacker = new BannerRenderer(null);
        updateBanners();
    }

    public void openForClaim(RecruitsClaim claim, int x, int y) {
        if (claim == null) {
            close();
            return;
        }

        this.visible = true;
        setClaim(claim);
        this.x = x;
        this.y = y;

        ensureWithinScreen();
    }

    public void setClaim(RecruitsClaim claim) {
        if (claim == null) {
            close();
            return;
        }

        this.currentClaim = claim;
        updateBanners();
    }

    private void updateBanners() {
        if (bannerRenderer == null || bannerRendererAttacker == null) return;

        if (currentClaim == null) {
            bannerRenderer.setRecruitsFaction(null);
            bannerRendererAttacker.setRecruitsFaction(null);
            return;
        }

        bannerRenderer.setRecruitsFaction(currentClaim.getOwnerFaction());

        if (currentClaim.attackingParties != null
                && !currentClaim.attackingParties.isEmpty()
                && currentClaim.attackingParties.get(0) != null) {
            bannerRendererAttacker.setRecruitsFaction(currentClaim.attackingParties.get(0));
        } else {
            bannerRendererAttacker.setRecruitsFaction(null);
        }
    }

    private void ensureWithinScreen() {
        if (x + width > parent.width) {
            x = parent.width - width - 10;
        }
        if (x < 10) {
            x = 10;
        }

        int bottomLimit = Math.max(10, parent.height - SAFE_BOTTOM_MARGIN);
        if (y + height > bottomLimit) {
            y = bottomLimit - height;
        }
        if (y < 10) {
            y = 10;
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!visible || currentClaim == null) return;

        Font font = parent.getMinecraft().font;

        guiGraphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        guiGraphics.renderOutline(x, y, width, height, PANEL_OUTLINE);

        int headerTextWidth = width - SIDE_BANNER_WIDTH - 18;
        drawTrimmedString(guiGraphics, font, currentClaim.getName(), x + 8, y + 8, headerTextWidth, VALUE_COLOR);
        drawTrimmedString(guiGraphics, font, getStatusText(), x + 8, y + 21, headerTextWidth, getStatusColor());
        drawTrimmedString(guiGraphics, font, getRelationText(), x + 8, y + 34, headerTextWidth, getRelationColor());
        renderBanners(guiGraphics);

        int dividerY = y + 55;
        guiGraphics.fill(x + 8, dividerY, x + width - 8, dividerY + 1, 0x66FFFFFF);

        int textY = y + 64;
        drawInfoRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.faction", getFactionName(), VALUE_COLOR);
        textY += 12;
        drawInfoRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.owner", getOwnerName(), VALUE_COLOR);
        textY += 12;
        drawInfoRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.members", Integer.toString(getFactionMemberCount()), VALUE_COLOR);
        textY += 12;
        drawInfoRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.claims", Integer.toString(getFactionClaimCount()), VALUE_COLOR);
        textY += 12;

        int chunkCount = getChunkCount();
        int maxClaimChunks = getMaxClaimChunks();
        int chunkTextColor = chunkCount >= maxClaimChunks ? WARNING_COLOR : VALUE_COLOR;
        drawInfoRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.chunks", chunkCount + "/" + maxClaimChunks, chunkTextColor);
        textY += 11;
        renderChunkProgress(guiGraphics, x + 8, textY, width - 16, 4, chunkCount, maxClaimChunks);
        textY += 13;

        drawPermissionRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.block_place", currentClaim.isBlockPlacementAllowed());
        textY += 12;
        drawPermissionRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.block_break", currentClaim.isBlockBreakingAllowed());
        textY += 12;
        drawPermissionRow(guiGraphics, font, textY, "gui.recruits.map.claim_info.block_use", currentClaim.isBlockInteractionAllowed());
    }

    private void renderBanners(GuiGraphics guiGraphics) {
        int bannerX = x + width - SIDE_BANNER_WIDTH + 2;
        int bannerY = y + 23;
        int bannerWidth = SIDE_BANNER_WIDTH - 6;
        int bannerHeight = 50;

        if (isUnderSiege()) {
            bannerRenderer.renderBanner(guiGraphics, bannerX + 4, bannerY, this.width, this.height, 19);
            bannerRendererAttacker.renderBanner(guiGraphics, bannerX + 28, bannerY, this.width, this.height, 19);
        } else {
            bannerRenderer.renderBanner(guiGraphics, bannerX + 10, bannerY, this.width, this.height, 29);
        }

        if (isUnderSiege()) {
            RenderSystem.setShaderTexture(0, SIEGE_ICON);
            int iconSize = 12;
            guiGraphics.blit(
                    SIEGE_ICON,
                    bannerX + bannerWidth / 2 - iconSize / 2,
                    bannerY + bannerHeight - iconSize,
                    0,
                    0,
                    iconSize,
                    iconSize,
                    iconSize,
                    iconSize);
        }
    }

    private void drawInfoRow(GuiGraphics guiGraphics, Font font, int rowY, String labelKey, String value, int valueColor) {
        String label = Component.translatable(labelKey).getString();
        int labelX = x + 8;
        int valueX = labelX + LABEL_COLUMN_WIDTH;
        int maxValueWidth = x + width - 8 - valueX;

        guiGraphics.drawString(font, trimToWidth(font, label, LABEL_COLUMN_WIDTH - 4), labelX, rowY, LABEL_COLOR);
        guiGraphics.drawString(font, trimToWidth(font, value, maxValueWidth), valueX, rowY, valueColor);
    }

    private void drawPermissionRow(GuiGraphics guiGraphics, Font font, int rowY, String labelKey, boolean allowed) {
        drawInfoRow(
                guiGraphics,
                font,
                rowY,
                labelKey,
                Component.translatable(allowed
                        ? "gui.recruits.map.claim_info.allowed"
                        : "gui.recruits.map.claim_info.denied").getString(),
                allowed ? ALLOWED_COLOR : DENIED_COLOR);
    }

    private void renderChunkProgress(
            GuiGraphics guiGraphics, int barX, int barY, int barWidth, int barHeight, int chunks, int maxChunks) {
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80333333);

        if (maxChunks > 0 && chunks > 0) {
            int fillWidth = Math.min(barWidth, Math.round(barWidth * (chunks / (float) maxChunks)));
            int fillColor = chunks >= maxChunks ? WARNING_COLOR : ALLOWED_COLOR;
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        }

        guiGraphics.renderOutline(barX, barY, barWidth, barHeight, 0x80777777);
    }

    private void drawTrimmedString(
            GuiGraphics guiGraphics, Font font, String text, int textX, int textY, int maxWidth, int color) {
        String trimmed = trimToWidth(font, text == null ? "" : text, maxWidth);
        guiGraphics.drawString(font, trimmed, textX, textY, color);
    }

    private String trimToWidth(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;

        String ellipsis = "...";
        int allowedWidth = Math.max(0, maxWidth - font.width(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(trimmed) > allowedWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private String getStatusText() {
        return Component.translatable(isUnderSiege()
                ? "gui.recruits.map.claim_info.status_siege"
                : "gui.recruits.map.claim_info.status_secure").getString();
    }

    private int getStatusColor() {
        return isUnderSiege() ? DENIED_COLOR : ALLOWED_COLOR;
    }

    private boolean isUnderSiege() {
        return currentClaim != null && currentClaim.isUnderSiege;
    }

    private String getFactionName() {
        RecruitsFaction faction = currentClaim.getOwnerFaction();
        if (faction == null) return Component.translatable("gui.recruits.map.claim_info.none").getString();
        return faction.getTeamDisplayName();
    }

    private String getOwnerName() {
        if (currentClaim.getPlayerInfo() == null) {
            return Component.translatable("gui.recruits.map.claim_info.unknown").getString();
        }
        return currentClaim.getPlayerInfo().getName();
    }

    private String getRelationText() {
        return Component.translatable(getRelationTranslationKey()).getString();
    }

    private int getRelationColor() {
        RecruitsFaction ownerFaction = currentClaim.getOwnerFaction();
        if (sameFaction(ClientManager.ownFaction, ownerFaction)) return ALLOWED_COLOR;
        if (ClientManager.ownFaction == null || ownerFaction == null) return VALUE_COLOR;

        return switch (ClientManager.getRelation(ClientManager.ownFaction.getStringID(), ownerFaction.getStringID())) {
            case ALLY -> ALLOWED_COLOR;
            case ENEMY -> DENIED_COLOR;
            case NEUTRAL -> WARNING_COLOR;
        };
    }

    private String getRelationTranslationKey() {
        RecruitsFaction ownerFaction = currentClaim.getOwnerFaction();
        if (sameFaction(ClientManager.ownFaction, ownerFaction)) {
            return "gui.recruits.map.claim_info.relation_own";
        }
        if (ClientManager.ownFaction == null || ownerFaction == null) {
            return "gui.recruits.map.claim_info.unknown";
        }

        RecruitsDiplomacyManager.DiplomacyStatus status =
                ClientManager.getRelation(ClientManager.ownFaction.getStringID(), ownerFaction.getStringID());
        return switch (status) {
            case ALLY -> "gui.recruits.map.claim_info.relation_ally";
            case ENEMY -> "gui.recruits.map.claim_info.relation_enemy";
            case NEUTRAL -> "gui.recruits.map.claim_info.relation_neutral";
        };
    }

    private int getFactionMemberCount() {
        RecruitsFaction faction = currentClaim.getOwnerFaction();
        return faction == null || faction.getMembers() == null ? 0 : faction.getMembers().size();
    }

    private int getFactionClaimCount() {
        RecruitsFaction faction = currentClaim.getOwnerFaction();
        if (faction == null) return 0;

        int count = 0;
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim != null && sameFaction(faction, claim.getOwnerFaction())) count++;
        }
        return count;
    }

    private int getChunkCount() {
        return currentClaim.getClaimedChunks() == null ? 0 : currentClaim.getClaimedChunks().size();
    }

    private int getMaxClaimChunks() {
        return ClientManager.configValueMaxClaimChunks > 0
                ? ClientManager.configValueMaxClaimChunks
                : RecruitsClaim.DEFAULT_MAX_SIZE;
    }

    private static boolean sameFaction(RecruitsFaction left, RecruitsFaction right) {
        if (left == null || right == null) return false;
        return Objects.equals(left.getStringID(), right.getStringID());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        return false;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void close() {
        this.visible = false;
        this.currentClaim = null;
        if (this.bannerRenderer != null) this.bannerRenderer.setRecruitsFaction(null);
        if (this.bannerRendererAttacker != null) this.bannerRendererAttacker.setRecruitsFaction(null);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        ensureWithinScreen();
    }
}
