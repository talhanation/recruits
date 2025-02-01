package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FastColor;

public class BlackShowingTextField extends AbstractWidget {
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    private final String text;
    private final int textXOffset;
    private final int textYOffset;
    public BlackShowingTextField(int x, int y, int width, int height, Component component) {
        this(x, y, width, height, 2, 0, component);
    }

    public BlackShowingTextField(int x, int y, int width, int height, int textXOffset, int textYOffset, Component component){
        super(x, y, width, height, TextComponent.EMPTY);
        this.text = component.getString();
        this.textXOffset = textXOffset;
        this.textYOffset = textYOffset;
    }
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, BG_FILL_SELECTED);
        drawString(poseStack, Minecraft.getInstance().font, text, this.x + textXOffset, this.y + textYOffset + (this.height - 8) / 2, 0xFFFFFF);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
