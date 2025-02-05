package com.talhanation.recruits.client.gui.team;


import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class RecruitsTeamEntry extends ListScreenEntryBase<RecruitsTeamEntry> {
    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    protected final Minecraft minecraft;
    protected final RecruitsTeamListScreen screen;
    protected final @NotNull RecruitsTeam team;
    protected final BannerRenderer bannerRenderer;

    public RecruitsTeamEntry(RecruitsTeamListScreen screen, @NotNull RecruitsTeam team) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.team = team;
        this.bannerRenderer = new BannerRenderer(team);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        int skinX = left + PADDING;
        int skinY = top + (height - SKIN_SIZE) / 2;
        int textX = skinX + SKIN_SIZE + PADDING;
        int textY = top + (height - minecraft.font.lineHeight) / 2;

        guiGraphics.fill(left, top, left + width, top + height, BG_FILL);

        renderElement(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, delta, skinX, skinY, textX, textY);
    }

    public void renderElement(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, int skinX, int skinY, int textX, int textY) {
        boolean selected = team.equals(screen.getSelected());
        if (selected) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL);
        }

        bannerRenderer.renderBanner(guiGraphics, left, top, width, height, 15);
        /*
        Integer teamColor = ChatFormatting.getById(team.getTeamColor()).getColor();
        int unitColor = TeamCreationScreen.RecruitColorID.get(team.getUnitColor());

        GuiComponent.fill(guiGraphics, left + 10, top , left + 200,top + 10, 0xFFFF0000);

        GuiComponent.fill(guiGraphics, left + 10, top + 20, left + 200,top + 10, 0x8000FF00);
         */
        guiGraphics.drawString(minecraft.font, team.getTeamDisplayName(), (float) textX + 10, (float) textY,  PLAYER_NAME_COLOR, false);
        guiGraphics.drawString(minecraft.font, getPlayersText(team.getPlayers()).getString(), (float) textX + 120, (float) textY, PLAYER_NAME_COLOR, false);
    }

    @Nullable
    public RecruitsTeam getTeamInfo() {
        return team;
    }

    @Override
    public ListScreenListBase<RecruitsTeamEntry> getList() {
        return screen.teamList;
    }

    private Component getPlayersText(int players){
        if(team.maxPlayers > 0){
            return Component.translatable("gui.recruits.team_list.players_count", players, team.maxPlayers);
        }
        else
            return  Component.translatable("gui.recruits.team_list.players_no_count", players);


    }
}
