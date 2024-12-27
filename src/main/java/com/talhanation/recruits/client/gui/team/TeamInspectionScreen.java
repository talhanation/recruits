package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.ConfirmScreen;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyTeamListScreen;
import com.talhanation.recruits.client.gui.player.IPlayerSelection;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.RecruitsPlayerEntry;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TeamInspectionScreen extends ListScreenBase implements IPlayerSelection {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/team/team_inspect.png");
    protected static final ResourceLocation LEADER_CROWN = new ResourceLocation(Main.MOD_ID, "textures/gui/image/leader_crown.png");
    private static final Component LEAVE_BUTTON = new TranslatableComponent("gui.recruits.team.leave");
    private static final Component DIPLOMACY_BUTTON = new TranslatableComponent("gui.recruits.team.diplomacy");
    private static final Component EDIT_BUTTON = new TranslatableComponent("gui.recruits.team.edit");
    private static final Component MANAGE_BUTTON = new TranslatableComponent("gui.recruits.team.manage");
    private static final Component BACK_BUTTON = new TranslatableComponent("gui.recruits.button.back");
    private static final Component MEMBERS_TEXT = new TranslatableComponent("gui.recruits.team.members");
    private static final Component PLAYERS_TEXT = new TranslatableComponent("gui.recruits.team.players");
    private static final Component NPCS_TEXT = new TranslatableComponent("gui.recruits.team.npcs");
    private static final Component LEADER_TEXT = new TranslatableComponent("gui.recruits.team.leader");
    private static final Component SELECT_LEADER = new TranslatableComponent("gui.recruits.team.select_leader");
    private static final Component SELECT_LEADER_TOOLTIP = new TranslatableComponent("gui.recruits.team.select_leader_tooltip_leaving");
    protected static final int HEADER_SIZE = 130;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 0;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 32;
    protected PlayersList playerList;
    protected int units;

    protected Screen parent;
    public RecruitsPlayerInfo selected;
    private Button backButton;
    private Button manageButton;
    private Button editButton;
    private Button diplomacyButton;
    private Button leaveButton;
    private final Player player;
    public static RecruitsTeam recruitsTeam;
    private BannerRenderer bannerRenderer;
    private SelectedPlayerWidget selectedPlayerWidget;
    private boolean postInit;
    private int gapBottom;
    private int gapTop;

    public TeamInspectionScreen(Screen parent, Player player){
        super(new TextComponent("TeamInspection"),236,0);
        this.parent = parent;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        postInit = false;
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateTeamInspaction());

        gapTop = (int) (this.height * 0.1);
        gapBottom = (int) (this.height * 0.1);

        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);
        ySize = HEADER_SIZE + units * UNIT_SIZE + FOOTER_SIZE;

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        if (playerList != null) {
            playerList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            playerList = new PlayersList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT,  this, PlayersList.FilterType.SAME_TEAM, player, true);
        }
        addWidget(playerList);

        backButton = new Button(guiLeft + 169, guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE, 60, 20, BACK_BUTTON,
                button -> {
                    minecraft.setScreen(parent);
                });
        addRenderableWidget(backButton);
    }


    public void postInit(){
        this.bannerRenderer = new BannerRenderer(recruitsTeam);
        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;
        this.selectedPlayerWidget = new SelectedPlayerWidget(font, guiLeft + 130, guiTop + 20, 100, 20, new TextComponent(""), () -> {});
        this.selectedPlayerWidget.setButtonActive(false);
        this.selectedPlayerWidget.setButtonVisible(false);
        this.selectedPlayerWidget.setPlayer(recruitsTeam.getTeamLeaderUUID(), recruitsTeam.getTeamLeaderName());
        addRenderableWidget(this.selectedPlayerWidget);

        boolean isTeamLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());

        editButton = new Button(guiLeft + 169, guiTop + 99, 60, 20, EDIT_BUTTON,
                button -> {
                    minecraft.setScreen(new TeamEditScreen(this, player, recruitsTeam));
                });
        editButton.visible = isTeamLeader;
        addRenderableWidget(editButton);

        diplomacyButton = new Button(guiLeft + 87, guiTop + 99, 60, 20, DIPLOMACY_BUTTON,
                button -> {
                    minecraft.setScreen(new DiplomacyTeamListScreen(this, isTeamLeader));
                });
        addRenderableWidget(diplomacyButton);

        manageButton = new Button(guiLeft + 87, buttonY, 60, 20, MANAGE_BUTTON,
                button -> {
                    minecraft.setScreen(new TeamManageScreen(this, player, recruitsTeam));
                });
        manageButton.visible = isTeamLeader;
        addRenderableWidget(manageButton);

        //leave team
        leaveButton = new Button(guiLeft + 7, buttonY, 60, 20, LEAVE_BUTTON,
            button -> {
                if(isTeamLeader){
                    minecraft.setScreen(new SelectPlayerScreen(this, player, SELECT_LEADER, SelectPlayerScreen.BUTTON_SELECT, SELECT_LEADER_TOOLTIP, false, PlayersList.FilterType.SAME_TEAM,
                            (playerInfo) -> {
                                    recruitsTeam.setTeamLeaderID(playerInfo.getUUID());
                                    recruitsTeam.setTeamLeaderName(playerInfo.getName());

                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveTeamSettings(recruitsTeam, recruitsTeam.getTeamName()));
                                    onClose();
                            }
                        ));
                }
                else {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
                    onClose();
                }
            });

        addRenderableWidget(leaveButton);

        postInit = true;
    }
    @Override
    public void tick() {
        super.tick();
        if(playerList != null){
            playerList.tick();
        }

        if(recruitsTeam != null && !postInit){
            this.postInit();
        }
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.playerList.setFocused(null);
        return flag;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    int x1 = 25;
    int y1 = 80;
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        super.render(poseStack, mouseX, mouseY, delta);
        if(bannerRenderer != null) bannerRenderer.renderBanner(poseStack, this.guiLeft + x1, guiTop + y1, this.width, this.height, 60);
    }
    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, HEADER_SIZE);
        for (int i = 0; i < units; i++) {
            blit(poseStack, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * i, 0, HEADER_SIZE, xSize, UNIT_SIZE);
        }
        blit(poseStack, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * units, 0, HEADER_SIZE + UNIT_SIZE, xSize, FOOTER_SIZE);
        blit(poseStack, guiLeft + 10, guiTop + HEADER_SIZE + 6 - 2, xSize, 0, 12, 12);
    }


    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        int textX = width / 2 - 48;
        int textY = guiTop + 25;
        int crownX = width / 2 - 6;
        int crownY = guiTop + 22;
        int numbersX = 65;
        if (!playerList.isEmpty()) {
            playerList.render(poseStack, mouseX, mouseY, delta);
        }

        if(recruitsTeam != null){
            int members = recruitsTeam.players + recruitsTeam.npcs;

            font.draw(poseStack, this.getTitle(), width / 2F - font.width(getTitle()) / 2F, guiTop + 5, 0xFF000000 | ChatFormatting.getById(recruitsTeam.getTeamColor()).getColor());

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShaderTexture(0, LEADER_CROWN);
            GuiComponent.blit(poseStack, crownX, crownY, 0, 0, 16, 16, 16, 16);

            font.draw(poseStack, LEADER_TEXT.getString(), textX, textY, 4210752);

            font.draw(poseStack, MEMBERS_TEXT.getString(), textX, textY + 25, 4210752);
            font.draw(poseStack, "" + members, textX + numbersX, textY + 25, 4210752);

            font.draw(poseStack, PLAYERS_TEXT.getString(), textX, textY + 40, 4210752);
            font.draw(poseStack, "" + recruitsTeam.players, textX + numbersX, textY + 40, 4210752);

            font.draw(poseStack, NPCS_TEXT.getString(), textX, textY + 55, 4210752);
            font.draw(poseStack, "" + recruitsTeam.npcs, textX + numbersX, textY + 55, 4210752);
        }
    }


    @Override
    public RecruitsPlayerInfo getSelected() {
        return selected;
    }

    @Override
    public ListScreenListBase<RecruitsPlayerEntry> getPlayerList() {
        return playerList;
    }

    @Override
    public Component getTitle() {
        String name = "";

        if(recruitsTeam != null){
            name = recruitsTeam.getTeamName();
        }
        return new TextComponent(name);
    }
}
