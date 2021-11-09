package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class RecruitInventoryScreen extends ScreenBase<RecruitInventoryContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png");

    private static final ITextComponent TEXT_HEALTH = new TranslationTextComponent("gui.recruits.health");
    private static final ITextComponent TEXT_LEVEL = new TranslationTextComponent("gui.recruits.level");

    private final AbstractRecruitEntity recruit;

    public RecruitInventoryScreen(RecruitInventoryContainer recruitContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, title);
        this.recruit = recruitContainer.getRecruit();

        imageWidth = 176;
        imageHeight = 202;
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        //drawHealth(matrixStack, recruit.getHealth());

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        //this.minecraft.getTextureManager().bind(RESOURCE_LOCATION);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventory(i + 50, j + 82, 30, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
    }

    public void drawHealth(MatrixStack matrixStack, float percent) {
        int scaled = (int) (72F * percent);
        int i = leftPos;
        int j = topPos;
        blit(matrixStack, i + 96, j + 20, 176, 0, scaled, 10);
    }

    public void drawLevel(MatrixStack matrixStack, float percent) {
        int scaled = (int) (72F * percent);
        int i = leftPos;
        int j = topPos;
        blit(matrixStack, i + 96, j + 20, 176, 10, scaled, 10);
    }

}
