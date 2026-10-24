package com.talhanation.recruits.client.gui.governor;

import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/** One claim in the claim list: banner, name, size and position, Governor status. */
@OnlyIn(Dist.CLIENT)
public class RecruitsClaimEntry extends ListScreenEntryBase<RecruitsClaimEntry> {
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    protected static final int INFO_COLOR = FastColor.ARGB32.color(255, 170, 170, 170);
    protected static final int FREE_COLOR = FastColor.ARGB32.color(255, 110, 210, 110);
    protected static final int CURRENT_COLOR = FastColor.ARGB32.color(255, 255, 200, 60);
    protected static final int TAKEN_COLOR = FastColor.ARGB32.color(255, 220, 120, 120);

    protected final Minecraft minecraft;
    protected final IClaimSelection screen;
    protected final @NotNull RecruitsClaim claim;
    protected final BannerRenderer bannerRenderer;

    public RecruitsClaimEntry(IClaimSelection screen, @NotNull RecruitsClaim claim) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.claim = claim;
        this.bannerRenderer = new BannerRenderer(claim.getOwnerFaction());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        boolean selected = screen.getSelected() != null && claim.getUUID().equals(screen.getSelected().getUUID());
        int bg = selected ? BG_FILL_SELECTED : hovered ? BG_FILL_HOVERED : BG_FILL;
        guiGraphics.fill(left, top, left + width, top + height, bg);

        bannerRenderer.renderBanner(guiGraphics, left + PADDING - 2, top, 24, height, 15);

        int textX = left + PADDING + 26;
        int lineY = top + 4;
        guiGraphics.drawString(minecraft.font, claim.getName(), textX, lineY, NAME_COLOR, false);

        ChunkPos center = claim.getCenter();
        String position = center != null ? (center.getMiddleBlockX() + " / " + center.getMiddleBlockZ()) : "";
        Component info = Component.translatable("gui.recruits.governor.claim_list.info", claim.getClaimedChunks().size(), position);
        guiGraphics.drawString(minecraft.font, info, textX, lineY + 11, INFO_COLOR, false);

        IClaimSelection.ClaimStatus status = screen.getStatus(claim);
        Component statusText = switch (status) {
            case FREE -> Component.translatable("gui.recruits.governor.claim_list.free");
            case CURRENT -> Component.translatable("gui.recruits.governor.claim_list.current");
            case TAKEN -> Component.translatable("gui.recruits.governor.claim_list.taken", screen.getGovernorName(claim));
        };
        int color = switch (status) {
            case FREE -> FREE_COLOR;
            case CURRENT -> CURRENT_COLOR;
            case TAKEN -> TAKEN_COLOR;
        };
        guiGraphics.drawString(minecraft.font, statusText, textX, lineY + 22, color, false);
    }

    @NotNull
    public RecruitsClaim getClaim() {
        return claim;
    }

    @Override
    public ListScreenListBase<RecruitsClaimEntry> getList() {
        return screen.getClaimList();
    }
}
