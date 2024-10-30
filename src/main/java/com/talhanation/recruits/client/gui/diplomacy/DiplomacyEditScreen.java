package com.talhanation.recruits.client.gui.diplomacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.network.MessageDiplomacyChangeStatus;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DiplomacyEditScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_popup.png");
    private static final Component TITLE = new TranslatableComponent("gui.recruits.diplomacy_edit.title");

    private Screen parent;
    private RecruitsTeam ownTeam;
    private  RecruitsTeam otherTeam;
    private RecruitsDiplomacyButton allyButton;
    private RecruitsDiplomacyButton neutralButton;
    private RecruitsDiplomacyButton enemyButton;
    public static RecruitsDiplomacyManager.DiplomacyStatus othersStance;
    public static RecruitsDiplomacyManager.DiplomacyStatus ownStance;

    public DiplomacyEditScreen(Screen parent, @NotNull RecruitsTeam ownTeam , @NotNull RecruitsTeam otherTeam) {
        super(TITLE, 195,124);
        this.ownTeam = ownTeam;
        this.otherTeam = otherTeam;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        hoverAreas.clear();
        clearWidgets();

        allyButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ALLY,guiLeft + 6, guiTop + ySize - 6, 21, 21, new TextComponent(""),
                button -> {
                    this.changeDiplomacyStatus(RecruitsDiplomacyManager.DiplomacyStatus.ALLY, otherTeam);
                }
        );
        addRenderableWidget(allyButton);

        neutralButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL,guiLeft + 27, guiTop + ySize - 6, 21, 21, new TextComponent(""),
                button -> {
                    this.changeDiplomacyStatus(RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL, otherTeam);
                }
        );
        addRenderableWidget(neutralButton);

        enemyButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ENEMY,guiLeft + 48, guiTop + ySize - 6, 21, 21, new TextComponent(""),
                button -> {
                    this.changeDiplomacyStatus(RecruitsDiplomacyManager.DiplomacyStatus.ENEMY, otherTeam);
                }
        );
        addRenderableWidget(enemyButton);

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
    }

    private void changeDiplomacyStatus(RecruitsDiplomacyManager.DiplomacyStatus status, RecruitsTeam otherTeam) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageDiplomacyChangeStatus(ownTeam, otherTeam, status));

    }

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        font.draw(poseStack, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getDiplomacyStatusIcon(othersStance));
        blit(poseStack, guiLeft + 5, guiTop + 5, 0, 0, 21, 21);
        /*
        if (mouseX >= groupTypeButton.x && mouseY >= groupTypeButton.y && mouseX < groupTypeButton.x + groupTypeButton.getWidth() && mouseY < groupTypeButton.y + groupTypeButton.getHeight()) {
            renderTooltip(poseStack, groupTypeButton.getTooltip(), mouseX, mouseY);
        }
        */

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getDiplomacyStatusIcon(ownStance));
        blit(poseStack, guiLeft + 5, guiTop + 5, 0, 0, 21, 21);
    }

    public ResourceLocation getDiplomacyStatusIcon(RecruitsDiplomacyManager.DiplomacyStatus status){
        ResourceLocation location;

        switch (status){
            default -> location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/neutral.png");
            case ALLY ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/ally.png");
            case ENEMY ->  location = new ResourceLocation(Main.MOD_ID, "textures/gui/image/enemy.png");
        }
        return location;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

}
