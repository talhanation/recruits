package com.talhanation.recruits.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.util.GameProfileUtils;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;


public class RecruitsPlayerEntry extends ListScreenEntryBase<RecruitsPlayerEntry> {
    protected static final int SKIN_SIZE = 24;
    protected static final int PADDING = 4;
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);

    protected final Minecraft minecraft;
    protected final IPlayerSelection screen;
    protected final @NotNull RecruitsPlayerInfo player;
    protected final BannerRenderer bannerRenderer;
    public RecruitsPlayerEntry(IPlayerSelection screen, @NotNull RecruitsPlayerInfo player) {
        this.minecraft = Minecraft.getInstance();
        this.screen = screen;
        this.player = player;
        this.bannerRenderer = new BannerRenderer(player.getRecruitsTeam());
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

    public void renderElement(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, int skinX, int skinY, int textX, int textY){
        boolean selected = screen.getSelected() != null && player.getUUID().equals(screen.getSelected().getUUID());
        if (selected) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            guiGraphics.fill(left, top, left + width, top + height, BG_FILL);
        }

        RenderSystem.setShaderTexture(0, GameProfileUtils.getSkin(player.getUUID()));
        guiGraphics.blit(GameProfileUtils.getSkin(player.getUUID()), skinX, skinY, SKIN_SIZE, SKIN_SIZE, 8, 8, 8, 8, 64, 64);
        RenderSystem.enableBlend();
        guiGraphics.blit(GameProfileUtils.getSkin(player.getUUID()), skinX, skinY, SKIN_SIZE, SKIN_SIZE, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
        guiGraphics.drawString(minecraft.font, player.getName(), (float) textX, (float) textY, PLAYER_NAME_COLOR, false);

        if(bannerRenderer != null){
            bannerRenderer.renderBanner(guiGraphics, left + 185, top, width, height, 15);
        }
    }
    @NotNull
    public RecruitsPlayerInfo getPlayerInfo() {
        return player;
    }

    @Override
    public ListScreenListBase<RecruitsPlayerEntry> getList() {
        return screen.getPlayerList();
    }
}
