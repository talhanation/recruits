package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageDebugGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class RenameRecruitScreen extends Screen {

    private static final int fontColor = 4210752;
    private EditBox editBox;
    private final Screen parent;
    private final AbstractRecruitEntity recruit;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/gui_small.png");
    private static final MutableComponent TEXT_CANCEL = Component.translatable("gui.recruits.groups.cancel");
    private static final MutableComponent TEXT_SAVE = Component.translatable("gui.recruits.groups.save");
    private static final MutableComponent TEXT_RENAME_RECRUIT = Component.translatable("gui.recruits.inv.rename");
    public RenameRecruitScreen(Screen parent, AbstractRecruitEntity recruit) {
        super(Component.literal(""));
        this.recruit = recruit;
        this.parent = parent;
        this.imageWidth = 250;
        this.imageHeight = 83;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        editBox = new EditBox(this.font, leftPos + 10, topPos + 20, 220, 20, Component.literal(""));
        if (recruit != null) {
            editBox.setValue(recruit.getName().getString());
        }
        this.addRenderableWidget(editBox);

        this.addRenderableWidget(new ExtendedButton(leftPos + 10, topPos + 55, 60, 20, TEXT_SAVE,
            button -> {
                String newName = editBox.getValue();
                if (!newName.isEmpty()) {
                    recruit.setCustomName(Component.literal(newName));

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(99, recruit.getUUID(), newName));

                    this.minecraft.setScreen(this.parent);
                }
        }));

        this.addRenderableWidget(new ExtendedButton(leftPos + 170, topPos + 55, 60, 20, TEXT_CANCEL, button -> {
            this.minecraft.setScreen(this.parent);
        }));
    }

    @Override
    public void tick() {
        super.tick();
        editBox.tick();
    }

    private void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, TEXT_RENAME_RECRUIT, leftPos + 10  , topPos + 5, fontColor, false);
    }
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        guiGraphics.blit(RESOURCE_LOCATION, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderForeground(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

