package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecruitsCategoryButton extends Button {

    private final ItemStack renderItem;
    private int x;
    private int y;
    public RecruitsCategoryButton(ItemStack renderItem, int xPos, int yPos, Component displayString, OnPress handler, OnTooltip tooltip) {
        super(xPos - 10, yPos - 10, 20, 20, displayString, handler, tooltip);
        this.renderItem = renderItem;
        this.x = xPos - 8;
        this.y = yPos - 8;
    }

    @Override
    public void render(PoseStack guiGraphics, int p_93658_, int p_93659_, float p_93660_) {
        super.render(guiGraphics, p_93658_, p_93659_, p_93660_);
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderGuiItem(renderItem, this.x, this.y);
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
