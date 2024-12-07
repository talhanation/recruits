package com.talhanation.recruits.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.widgets.ListScreenEntryBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.util.GameProfileUtils;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
    public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
        int skinX = left + PADDING;
        int skinY = top + (height - SKIN_SIZE) / 2;
        int textX = skinX + SKIN_SIZE + PADDING;
        int textY = top + (height - minecraft.font.lineHeight) / 2;

        GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL);

        renderElement(poseStack, index, top, left, width, height, mouseX, mouseY, hovered, delta, skinX, skinY, textX, textY);
    }

    public void renderElement(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, int skinX, int skinY, int textX, int textY){
        boolean selected = player.equals(screen.getSelected());
        if (selected) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_SELECTED);
        } else if (hovered) {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL_HOVERED);
        } else {
            GuiComponent.fill(poseStack, left, top, left + width, top + height, BG_FILL);
        }

        RenderSystem.setShaderTexture(0, GameProfileUtils.getSkin(player.getUUID()));
        GuiComponent.blit(poseStack, skinX, skinY, SKIN_SIZE, SKIN_SIZE, 8, 8, 8, 8, 64, 64);
        RenderSystem.enableBlend();
        GuiComponent.blit(poseStack, skinX, skinY, SKIN_SIZE, SKIN_SIZE, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
        minecraft.font.draw(poseStack, player.getName(), (float) textX, (float) textY, PLAYER_NAME_COLOR);

        if(bannerRenderer != null){
            bannerRenderer.renderBanner(poseStack, left + 185, top, width, height, 15);
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
