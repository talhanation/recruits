package com.talhanation.recruits.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class RecruitsGroupButton extends ExtendedButton {

    private RecruitsGroup group;

    public RecruitsGroupButton(RecruitsGroup group, int xPos, int yPos, int width, int height, Component displayString, OnPress handler) {
        super(xPos, yPos, width, height, displayString, handler);
        this.group = group;
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
        return this.visible && p_93681_ >= (double)this.getX() && p_93682_ >= (double)this.getY() && p_93681_ < (double)(this.getX() + this.width) && p_93682_ < (double)(this.getY() + this.height);
    }

    public RecruitsGroup getGroup() {
        return group;
    }
}
