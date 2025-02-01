package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.util.GameProfileUtils;
import com.talhanation.recruits.world.RecruitsTeam.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SelectedPlayerWidget extends AbstractWidget {
    private final int x, y, width, height;
    private final Button actionButton;
    private final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private final int BACKGROUND_COLOR = FastColor.ARGB32.color(255, 0, 0, 0);

    private final Font font;
    @Nullable
    private UUID playerUUID;
    @Nullable
    private String playerName;

    public SelectedPlayerWidget(Font font, int x, int y, int width, int height, Component buttonLabel, Runnable onPress) {
        super(x, y, width, height, Component.literal(""));
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
        this.actionButton = new Button(x + width - 20, y, 20, 20, buttonLabel, button ->{
            onPress.run();
        });
    }

    public void setButtonActive(boolean x){
        this.actionButton.active = x;
    }

    public void setButtonVisible(boolean x){
        this.actionButton.visible = x;
    }

    public void setPlayer(@Nullable UUID playerUUID, @Nullable String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (playerUUID != null && playerName != null) {
            fill(poseStack, x, y, x + width, y + height, BACKGROUND_COLOR);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, GameProfileUtils.getSkin(playerUUID));
            blit(poseStack, x, y, 20, 20, 8, 8, 8, 8, 64, 64);
            RenderSystem.enableBlend();
            blit(poseStack, x, y, 20, 20, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();

            GuiComponent.drawString(poseStack, font, playerName, x + 25, y + 6, PLAYER_NAME_COLOR);

            actionButton.render(poseStack, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        if(actionButton.isMouseOver(x,y) && actionButton.active && actionButton.visible) actionButton.onClick(x, y);
        return super.mouseClicked(x,y,i);
    }

    @Override
    public void onClick(double x, double y) {
        if(actionButton.isMouseOver(x,y) && actionButton.active && actionButton.visible) actionButton.onClick(x, y);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {
    }
}

