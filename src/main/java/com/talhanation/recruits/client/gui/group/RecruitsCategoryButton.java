package com.talhanation.recruits.client.gui.group;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class RecruitsCategoryButton extends ExtendedButton {

    private ItemStack renderItem;
    private int x;
    private int y;
    public RecruitsCategoryButton(ItemStack renderItem, int xPos, int yPos, Component displayString, OnPress handler) {
        super(xPos - 10, yPos - 10, 20, 20, displayString, handler);
        this.renderItem = renderItem;
        this.x = xPos - 8;
        this.y = yPos - 8;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int p_93658_, int p_93659_, float p_93660_) {
        super.render(guiGraphics, p_93658_, p_93659_, p_93660_);
        guiGraphics.renderFakeItem(renderItem, this.x, this.y);
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
}
