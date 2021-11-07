package com.talhanation.recruits.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;


import java.awt.Color;

/*
public class RecruitGui extends ScreenBase<RecruitContainer> {

    private AbstractRecruitEntity recruit;
    private PlayerInventory playerInventory;

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_recruit.png");
    private static final int FONT_COLOR = Color.DARK_GRAY.getRGB();

    protected Button buttonStart;
    protected Button buttonStop;

    public RecruitGui(RecruitContainer recruit, PlayerInventory playerInventory, ITextComponent title) {
        super(GUI_TEXTURE, recruit, playerInventory, title);
        this.recruit = recruit.getRecruit();
        this.playerInventory = playerInventory;

        imageWidth = 140;
        imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        buttonStart = addButton(new Button((width / 2) - 20, topPos + 100, 40, 20, new TranslationTextComponent("button.recruits.start"), button -> {
            recruit.setListen(true);
            recruit.sendListenToServer(true);
        }));

        buttonStop = addButton(new Button((width / 2) - 20, topPos + 100, 40, 20, new TranslationTextComponent("button.recruits.start"), button -> {
            recruit.setListen(false);
            recruit.sendListenToServer(false);
        }));

    }


    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        // buttons
        buttonStart.active = !recruit.getListen();
        buttonStop.active = recruit.getListen();
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        font.draw(matrixStack, recruit.getRecruitName(), 7, 61, FONT_COLOR);

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}

 */