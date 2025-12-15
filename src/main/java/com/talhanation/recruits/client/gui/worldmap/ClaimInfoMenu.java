package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

public class ClaimInfoMenu {
    private final WorldMapScreen parent;
    private boolean visible = false;
    private RecruitsClaim currentClaim;
    private BannerRenderer bannerRenderer;
    public int x, y;
    public int width = 120, height = 200;

    public ClaimInfoMenu(WorldMapScreen parent) {
        this.parent = parent;
    }

    public void init() {
        this.bannerRenderer = new BannerRenderer(null);
    }

    public void openForClaim(RecruitsClaim claim, int x, int y) {
        this.currentClaim = claim;
        this.visible = true;
        bannerRenderer.setRecruitsTeam(claim.getOwnerFaction());

        this.x = x;
        this.y = y;

        ensureWithinScreen();
    }

    private void ensureWithinScreen() {
        if (x + width > parent.width) {
            x = parent.width - width - 10;
        }
        if (x < 10) {
            x = 10;
        }
        if (y + height > parent.height) {
            y = parent.height - height - 10;
        }
        if (y < 10) {
            y = 10;
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!visible || currentClaim == null) return;

        guiGraphics.fill(x, y, x + width, y + height, 0xCC000000);
        guiGraphics.renderOutline(x, y, width, height, 0xFFFFFFFF);

        guiGraphics.drawCenteredString(parent.getMinecraft().font, currentClaim.getName(), x + width / 2, y + 5, 0xFFFFFF);

        bannerRenderer.renderBanner(guiGraphics, x - 7 + width / 2, y + 70,  this.width, this.height, 50);

        int textY = y + 120;
        guiGraphics.drawString(parent.getMinecraft().font,
                "Faction: " + (currentClaim.getOwnerFaction() != null ?
                        currentClaim.getOwnerFaction().getTeamDisplayName() : "None"),
                x + 5, textY, 0xFFFFFF);

        textY += 15;
        guiGraphics.drawString(parent.getMinecraft().font,
                "Owner: " + (currentClaim.getPlayerInfo() != null ?
                        currentClaim.getPlayerInfo().getName() : "Unknown"),
                x + 5, textY, 0xFFFFFF);

        textY += 15;
        guiGraphics.drawString(parent.getMinecraft().font,
                "Block-Place: " + currentClaim.isBlockPlacementAllowed(),
                x + 5, textY, 0xFFFFFF);

        textY += 15;
        guiGraphics.drawString(parent.getMinecraft().font,
                "Block-Break: " + currentClaim.isBlockBreakingAllowed(),
                x + 5, textY, 0xFFFFFF);

        textY += 15;
        guiGraphics.drawString(parent.getMinecraft().font,
                "Block-Use: " + currentClaim.isBlockInteractionAllowed(),
                x + 5, textY, 0xFFFFFF);

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
        this.bannerRenderer.setRecruitsTeam(null);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

