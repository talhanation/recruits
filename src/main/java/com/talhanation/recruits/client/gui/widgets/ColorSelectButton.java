package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.gui.component.ActivateableButton;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.network.chat.Component;

import java.awt.*;


@OnlyIn(Dist.CLIENT)
public class ColorSelectButton extends ActivateableButton {
    private final int color;

    public ColorSelectButton(int color, int xPos, int yPos, int width, int height, Component displayString, OnPress handler, OnTooltip tooltip) {
        super(xPos, yPos, width, height, displayString, handler, tooltip);
        this.color = color;
    }

    @Override
    public boolean mouseClicked(double p_93641_, double p_93642_, int p_93643_) {
        if (this.visible) {
            if (this.isValidClickButton(p_93643_)) {
                boolean flag = this.clicked(p_93641_, p_93642_);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(p_93641_, p_93642_);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected boolean clicked(double p_93681_, double p_93682_) {
        return this.visible && p_93681_ >= (double)this.x && p_93682_ >= (double)this.y && p_93681_ < (double)(this.x + this.width) && p_93682_ < (double)(this.y + this.height);
    }


    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(poseStack, mouseX, mouseY, partialTicks);

        fillGradient(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, color, color);
    }






}
