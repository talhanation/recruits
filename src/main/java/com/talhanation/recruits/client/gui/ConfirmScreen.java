package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class ConfirmScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_small.png");
    private final Runnable yesAction;
    private final Runnable noAction;
    private final Runnable backAction;
    private final Component text;

    private static final MutableComponent BUTTON_YES = Component.translatable("gui.recruits.button.Yes");
    private static final MutableComponent BUTTON_NO = Component.translatable("gui.recruits.button.No");
    private static final MutableComponent BUTTON_BACK = Component.translatable("gui.recruits.button.back");


    public ConfirmScreen(Component title, Component text, Runnable yesAction, Runnable noAction) {
        this(title, text, yesAction, noAction, null);
    }
    public ConfirmScreen(Component title, Component text, Runnable yesAction, Runnable noAction, Runnable backAction) {
        super(title, 246,84);
        this.yesAction = yesAction;
        this.noAction = noAction;
        this.backAction = backAction;
        this.text = text;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 75;
        int buttonHeight = 20;


        this.addRenderableWidget(new ExtendedButton(guiLeft + 7, guiTop + ySize - 27, buttonWidth, buttonHeight, BUTTON_YES, button -> {
            this.yesAction.run();
            onClose();
        }));

        if(backAction != null){
            this.addRenderableWidget(new ExtendedButton(guiLeft + 7 + 75 + 2, guiTop + ySize - 27, buttonWidth, buttonHeight, BUTTON_NO, button -> {
                this.noAction.run();
                onClose();
            }));

            this.addRenderableWidget(new ExtendedButton(guiLeft + 7 + 150 + 4, guiTop + ySize - 27, buttonWidth, buttonHeight, BUTTON_BACK, button -> {
                this.backAction.run();

            }));
        }
        else {
            this.addRenderableWidget(new ExtendedButton(guiLeft + 7 + 150 + 4, guiTop + ySize - 27, buttonWidth, buttonHeight, BUTTON_NO, button -> {
                this.noAction.run();
                onClose();
            }));
        }

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, title, guiLeft + xSize / 2 - font.width(title) / 2, guiTop + 7, FONT_COLOR, false);
        int maxWidth = xSize - 20; // xSize minus Padding


        List<FormattedCharSequence> lines = font.split(text, maxWidth);

        int lineHeight = 10;
        int yPosition = guiTop + 27;

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, guiLeft + xSize / 2 - font.width(line) / 2, yPosition, FONT_COLOR, false);
            yPosition += lineHeight;
        }
    }
}
