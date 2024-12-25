package com.talhanation.recruits.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ActivateableButton extends Button {
    public ActivateableButton(int p_93728_, int p_93729_, int p_93730_, int p_93731_, Component p_93732_, OnPress p_93733_, OnTooltip p_93734_) {
        super(p_93728_, p_93729_, p_93730_, p_93731_, p_93732_, p_93733_, p_93734_);
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
}
