package com.talhanation.recruits.client.gui.diplomacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.network.MessageChangeDiplomacyStatus;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.jetbrains.annotations.NotNull;


public class DiplomacyEditScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.diplomacy_edit.title");
    protected static final Component BUTTON_CONFIRM = Component.translatable("gui.recruits.button.confirm");
    protected static final Component BUTTON_BACK = Component.translatable("gui.recruits.button.back");
    private Screen parent;
    private RecruitsFaction otherTeam;
    private RecruitsDiplomacyButton allyButton;
    private RecruitsDiplomacyButton neutralButton;
    private RecruitsDiplomacyButton enemyButton;
    public RecruitsDiplomacyManager.DiplomacyStatus othersStance;
    public RecruitsDiplomacyManager.DiplomacyStatus ownStance;
    public RecruitsDiplomacyManager.DiplomacyStatus newStance;
    protected final BannerRenderer bannerOwn;
    protected final BannerRenderer bannerOther;
    private boolean stanceChanged;
    private Button confirmButton;
    private Button backButton;
    private boolean isLeader;

    public DiplomacyEditScreen(Screen parent, @NotNull RecruitsFaction otherTeam) {
        super(TITLE, 195,160);
        this.otherTeam = otherTeam;
        this.parent = parent;
        this.bannerOwn = new BannerRenderer(ClientManager.ownFaction);
        this.bannerOther = new BannerRenderer(otherTeam);
    }

    @Override
    protected void init() {
        super.init();
        if(ClientManager.ownFaction == null) return;
        isLeader = ClientManager.ownFaction.teamLeaderID.equals(Minecraft.getInstance().player.getUUID());
        othersStance = ClientManager.getRelation(otherTeam.getStringID(), ClientManager.ownFaction.getStringID());
        ownStance = ClientManager.getRelation(ClientManager.ownFaction.getStringID(), otherTeam.getStringID());
        hoverAreas.clear();
        newStance = ownStance;
        setButtons();
    }

    @Override
    public void tick() {
        super.tick();
        othersStance = ClientManager.getRelation(otherTeam.getStringID(), ClientManager.ownFaction.getStringID());
        ownStance = ClientManager.getRelation(ClientManager.ownFaction.getStringID(), otherTeam.getStringID());
    }

    private void setButtons(){
        clearWidgets();
        boolean hasTreaty = ClientManager.ownFaction != null &&
                ClientManager.hasTreaty(ClientManager.ownFaction.getStringID(), otherTeam.getStringID());

        allyButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ALLY,60 + guiLeft + 6, guiTop + ySize - 6 - 125, 21, 21,  Component.literal(""),
                button -> {
                    this.newStance = RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
                    stanceChanged = this.newStance != ownStance;
                    setButtons();
                }
        );
        allyButton.visible = !hasTreaty;
        addRenderableWidget(allyButton);

        neutralButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL,60 + guiLeft + 27, guiTop + ySize - 6 - 125, 21, 21, Component.literal(""),
                button -> {
                    this.newStance = RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL;
                    stanceChanged = this.newStance != ownStance;
                    setButtons();
                }
        );
        neutralButton.visible = !hasTreaty;
        addRenderableWidget(neutralButton);

        enemyButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ENEMY,60 + guiLeft + 48, guiTop + ySize - 6 - 125, 21, 21, Component.literal(""),
                button -> {
                    this.newStance = RecruitsDiplomacyManager.DiplomacyStatus.ENEMY;
                    stanceChanged = this.newStance != ownStance;
                    setButtons();
                }
        );

        addRenderableWidget(enemyButton);

        this.allyButton.active = newStance == RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
        this.neutralButton.active = newStance == RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL;
        this.enemyButton.active = newStance == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY;

        this.allyButton.visible = isLeader;
        this.neutralButton.visible = isLeader;
        this.enemyButton.visible = isLeader;

        if (hasTreaty) {
            this.allyButton.visible = false;
            this.neutralButton.visible = false;
            this.enemyButton.visible = false;
            this.allyButton.active = false;
            this.neutralButton.active = false;
            this.enemyButton.active = false;
        }

        confirmButton = new ExtendedButton(guiLeft + 6, guiTop + ySize - 18 - 7, 90, 20, BUTTON_CONFIRM,
                button -> {
                    this.changeDiplomacyStatus(newStance, otherTeam);
                    this.ownStance = newStance;
                    stanceChanged = false;
                    setButtons();
                });

        confirmButton.active = stanceChanged && !hasTreaty;
        confirmButton.visible = isLeader;
        addRenderableWidget(confirmButton);

        backButton = new ExtendedButton(guiLeft + 98, guiTop + ySize - 18 - 7, 90, 20, BUTTON_BACK,
                button -> {
                    minecraft.setScreen(parent);
                });
        addRenderableWidget(backButton);
    }

    private void changeDiplomacyStatus(RecruitsDiplomacyManager.DiplomacyStatus status, RecruitsFaction otherTeam) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageChangeDiplomacyStatus(ClientManager.ownFaction, otherTeam, status));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }
    int x3 = 120;
    int y3 = 90;
    int x4 = 60;
    int y4 = 90;
    int x5 = -70;
    int y5 = -30;
    int x6 = 70;
    int y6 = -30;
    int x7 = 35;
    int y7 = -100;
    int x8 = -25;
    int y8 = -100;
    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if(ClientManager.ownFaction == null) return;

        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR, false);

        guiGraphics.drawString(font, ClientManager.ownFaction.getTeamDisplayName(), x5 + guiLeft + xSize / 2 - font.width(ClientManager.ownFaction.getTeamDisplayName()) / 2, guiTop + 7 - y5, FONT_COLOR, false);
        guiGraphics.drawString(font, otherTeam.getTeamDisplayName(), x6 + guiLeft + xSize / 2 - font.width(otherTeam.getTeamDisplayName()) / 2, guiTop + 7 - y6, FONT_COLOR, false);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getDiplomacyStatusIcon(othersStance));
        guiGraphics.blit(getDiplomacyStatusIcon(othersStance), this.guiLeft + x3, guiTop + ySize - y3, 0, 0, 21, 21, 21, 21);
        guiGraphics.drawString(font, othersStance.name(), x7 + guiLeft + xSize / 2 - font.width(othersStance.name()) / 2, guiTop + 7 - y7, FONT_COLOR,false);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, getDiplomacyStatusIcon(newStance));
        guiGraphics.blit(getDiplomacyStatusIcon(newStance), this.guiLeft + x4, guiTop + ySize - y4, 0, 0, 21, 21, 21, 21);
        guiGraphics.drawString(font, newStance.name(), x8 + guiLeft + xSize / 2 - font.width(newStance.name()) / 2,  guiTop + 7 - y8, FONT_COLOR, false);

        boolean hasTreaty = ClientManager.hasTreaty(ClientManager.ownFaction.getStringID(), otherTeam.getStringID());
        if (hasTreaty) {
            long remainingMs = ClientManager.getTreatyRemainingMillis(ClientManager.ownFaction.getStringID(), otherTeam.getStringID());
            long totalMinutes = remainingMs / 60000;
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            String treatyText = "Treaty: " + hours + "h " + minutes + 1 + "min remaining";
            guiGraphics.drawCenteredString(font, treatyText, guiLeft + xSize / 2, guiTop + 30, 0xFF55FF55);
        }
    }
    int x1 = 15;
    int y1 = 75;
    int x2 = 160;
    int y2 = 75;
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        bannerOwn.renderBanner(guiGraphics, this.guiLeft + x1, guiTop + ySize - y1, this.width, this.height, 40);
        bannerOther.renderBanner(guiGraphics, this.guiLeft + x2, guiTop + ySize - y2, this.width, this.height, 40);
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

    }

}
