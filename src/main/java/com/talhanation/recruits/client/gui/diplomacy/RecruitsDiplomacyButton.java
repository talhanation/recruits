package com.talhanation.recruits.client.gui.diplomacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class RecruitsDiplomacyButton extends ExtendedButton {

    private final RecruitsDiplomacyManager.DiplomacyStatus status;

    public RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus status, int xPos, int yPos, int width, int height, Component displayString, OnPress handler) {
        super(xPos, yPos, width, height, displayString, handler);
        this.status = status;
    }

    private static Component createDisplayString(RecruitsGroup group) {
        return Component.literal(group.getName() + " (" + group.getCount() + ")");
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
        return this.visible && p_93681_ >= (double) this.getX() && p_93682_ >= (double) this.getY() && p_93681_ < (double)(this.getX() + this.width) && p_93682_ < (double)(this.getY() + this.height);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        super.renderWidget(guiGraphics, mouseX, mouseY, f);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getTextureLocation());
        guiGraphics.blit(getTextureLocation(), this.getX(), this.getY(), 0, 0, 21, 21, 21, 21);
    }

    private ResourceLocation getTextureLocation() {
        ResourceLocation location;

        switch (this.status){
            default -> location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/neutral.png");
            case ALLY ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/ally.png");
            case ENEMY ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/enemy.png");
        }
        return location;
    }


}
