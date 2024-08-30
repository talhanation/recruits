package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class RecruitsFormationButton extends ExtendedButton {
    private final CommandScreen.Formation formation;
    public RecruitsFormationButton(CommandScreen.Formation formation, int xPos, int yPos, OnPress handler) {
        super(xPos - 10, yPos - 10, 21, 21, Component.empty(), handler);
        this.formation = formation;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        super.renderWidget(guiGraphics, mouseX, mouseY, f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        guiGraphics.blit(getTextureLocation(), getX(), getY(), 0, 0, 21, 21, 21, 21);
    }

    private ResourceLocation getTextureLocation() {
        ResourceLocation location;
        switch (this.formation){
            default -> location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/none.png");
            case LINE ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/line.png");
            case SQUARE ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/square.png");
            case TRIANGLE ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/triangle.png");
            case HCIRCLE ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/hcircle.png");
            case HSQUARE ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/hsquare.png");
            case VFORM ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/vform.png");
        }
        return location;
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
