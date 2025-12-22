package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ClaimInfoMenu {
    private static final ResourceLocation SIEGE_ICON = new ResourceLocation("recruits:textures/gui/image/enemy.png");
    private final WorldMapScreen parent;
    private boolean visible = false;
    private boolean underSiege;
    private RecruitsClaim currentClaim;
    private BannerRenderer bannerRenderer;
    private BannerRenderer bannerRendererAttacker;
    public int x, y;
    public int width = 120, height = 200;

    public ClaimInfoMenu(WorldMapScreen parent) {
        this.parent = parent;
    }

    public void init() {
        this.bannerRenderer = new BannerRenderer(null);
        this.bannerRendererAttacker = new BannerRenderer(null);
    }

    public void openForClaim(RecruitsClaim claim, int x, int y) {
        this.currentClaim = claim;
        this.visible = true;
        bannerRenderer.setRecruitsTeam(claim.getOwnerFaction());
        this.underSiege = claim.isUnderSiege;

        if(!claim.attackingParties.isEmpty() && claim.attackingParties.get(0) != null){
            bannerRendererAttacker.setRecruitsTeam(claim.attackingParties.get(0));
        }

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

        if(this.underSiege){
            bannerRenderer.renderBanner(guiGraphics, x - 7 - 35 + width / 2, y + 65,  this.width, this.height, 40);
            bannerRendererAttacker.renderBanner(guiGraphics, x - 7 + 30 + width / 2, y + 65,  this.width, this.height, 40);
            RenderSystem.setShaderTexture(0, SIEGE_ICON);
            int iconSize = 18;
            guiGraphics.blit(SIEGE_ICON, x + width / 2 - iconSize / 2, y + 50, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }
        else{
            bannerRenderer.renderBanner(guiGraphics, x - 7 + width / 2, y + 70,  this.width, this.height, 50);
        }

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

