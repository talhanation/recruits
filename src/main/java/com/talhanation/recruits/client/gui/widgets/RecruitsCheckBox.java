package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class RecruitsCheckBox extends Checkbox {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
    private static final int TEXT_COLOR = 14737632;
    private final Consumer<Boolean> onToggle;

    public RecruitsCheckBox(int x, int y, int width, int height, Component label, boolean selected, Consumer<Boolean> onToggle) {
        this(x, y, width, height, label, selected, true, onToggle);
    }

    public RecruitsCheckBox(int x, int y, int width, int height, Component label, boolean selected, boolean showLabel, Consumer<Boolean> onToggle) {
        super(x, y, width, height, label, selected, showLabel);
        this.onToggle = onToggle;

    }

    @Override
    public void onPress() {
        super.onPress();
        if (onToggle != null) {
            onToggle.accept(this.selected());
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        RenderSystem.enableDepthTest();

        int alpha = Mth.ceil(this.alpha * 255.0F);
        int bgColor = (0x80 << 24); // 0x80 = 128 = 50% Alpha
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, this.getX(), this.getY(), this.isFocused() ? 20.0F : 0.0F, this.selected() ? 20.0F : 0.0F, 20, this.height, 64, 64);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textX = this.getX() + 24;
        int textY = this.getY() + (this.height - 8) / 2;
        graphics.drawString(font, this.getMessage(), textX, textY, TEXT_COLOR | (alpha << 24));
    }
}

