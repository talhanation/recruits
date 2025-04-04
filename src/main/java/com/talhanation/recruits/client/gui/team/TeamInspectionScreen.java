package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.MessageLeaveTeam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import com.talhanation.recruits.TeamEvents;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class TeamInspectionScreen extends ListScreenBase implements IPlayerSelection {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/team/team_inspect.png");
    protected static final ResourceLocation LEADER_CROWN = new ResourceLocation(Main.MOD_ID, "textures/gui/image/leader_crown.png");
    private static final Component LEAVE_BUTTON = Component.translatable("gui.recruits.team.leave");
    private static final Component DELETE_BUTTON = Component.translatable("gui.recruits.team.delete_team");
    private static final Component DIPLOMACY_BUTTON = Component.translatable("gui.recruits.team.diplomacy");
    private static final Component EDIT_BUTTON = Component.translatable("gui.recruits.team.edit");
    private static final Component MANAGE_BUTTON = Component.translatable("gui.recruits.team.manage");
    private static final Component BACK_BUTTON = Component.translatable("gui.recruits.button.back");
    private static final Component MEMBERS_TEXT = Component.translatable("gui.recruits.team.members");
    private static final Component PLAYERS_TEXT = Component.translatable("gui.recruits.team.players");
    private static final Component NPCS_TEXT = Component.translatable("gui.recruits.team.npcs");
    private static final Component LEADER_TEXT = Component.translatable("gui.recruits.team.leader");
    private static final Component SELECT_LEADER = Component.translatable("gui.recruits.team.select_leader");
    private static final Component SELECT_LEADER_TOOLTIP = Component.translatable("gui.recruits.team.select_leader_tooltip_leaving");
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
    public static boolean isEditingAllowed;
    public static boolean isManagingAllowed;

    public TeamInspectionScreen(Screen parent, Player player){
        super(Component.literal("TeamInspection"),236,0);
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

        if (playerList != null) {
            playerList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            playerList = new PlayersList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT,  this, PlayersList.FilterType.SAME_TEAM, player, true);
        }
        addWidget(playerList);

        backButton = new ExtendedButton(guiLeft + 169, guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE, 60, 20, BACK_BUTTON,
                button -> {
                    minecraft.setScreen(parent);
                });
        addRenderableWidget(backButton);
    }


    public void postInit(){
        this.bannerRenderer = new BannerRenderer(recruitsTeam);
        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;
        this.selectedPlayerWidget = new SelectedPlayerWidget(font, guiLeft + 130, guiTop + 20, 100, 20, Component.literal(""), () -> {});
        this.selectedPlayerWidget.setButtonActive(false);
        this.selectedPlayerWidget.setButtonVisible(false);
        this.selectedPlayerWidget.setPlayer(recruitsTeam.getTeamLeaderUUID(), recruitsTeam.getTeamLeaderName());
        addRenderableWidget(this.selectedPlayerWidget);

        boolean isTeamLeader = recruitsTeam.getTeamLeaderUUID().equals(player.getUUID());

        editButton = new ExtendedButton(guiLeft + 169, guiTop + 99, 60, 20, EDIT_BUTTON,
                button -> {
                    TeamEditScreen.leaderInfo = null;
                    TeamEvents.openTeamEditScreen(player);
            //minecraft.setScreen(new TeamEditScreen(this, player, recruitsTeam));
                });
        editButton.visible = isTeamLeader && isEditingAllowed;
        addRenderableWidget(editButton);

        diplomacyButton = new ExtendedButton(guiLeft + 87, guiTop + 99, 60, 20, DIPLOMACY_BUTTON,
                button -> {
                    minecraft.setScreen(new DiplomacyTeamListScreen(this, isTeamLeader));
                });
        addRenderableWidget(diplomacyButton);

        manageButton = new ExtendedButton(guiLeft + 87, buttonY, 60, 20, MANAGE_BUTTON,
                button -> {
                    minecraft.setScreen(new TeamManageScreen(this, player, recruitsTeam));
                });
        manageButton.visible = isTeamLeader && isManagingAllowed;
        addRenderableWidget(manageButton);

        boolean deleteActive = isTeamLeader && playerList != null && playerList.size() <= 1;
        //leave team
        leaveButton = new ExtendedButton(guiLeft + 7, buttonY, 60, 20, deleteActive ? DELETE_BUTTON : LEAVE_BUTTON,
            button -> {
                if(isTeamLeader){
                    if(deleteActive){
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());

                        minecraft.setScreen(new TeamMainScreen(player));
                        return;
                    }

                    Screen selectPlayerScreen = new SelectPlayerScreen(this, player, SELECT_LEADER, SelectPlayerScreen.BUTTON_SELECT, SELECT_LEADER_TOOLTIP, false, PlayersList.FilterType.SAME_TEAM,
                            (playerInfo) -> {
                                RecruitsTeam team = playerInfo.getRecruitsTeam();
                                team.setTeamLeaderID(playerInfo.getUUID());
                                team.setTeamLeaderName(playerInfo.getName());

                                Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveTeamSettings(team, 0));
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
                                onClose();
                            }
                    );
                    minecraft.setScreen(selectPlayerScreen);

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
        Lighting.setupFor3DItems();

    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.playerList.setFocused(null);
        return flag;
    }

    public void removed() {
        super.removed();
        recruitsTeam = null;
    }

    int x1 = 25;
    int y1 = 80;
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if(bannerRenderer != null) bannerRenderer.renderBanner(guiGraphics, this.guiLeft + x1, guiTop + y1, this.width, this.height, 60);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, HEADER_SIZE);
        for (int i = 0; i < units; i++) {
            guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * i, 0, HEADER_SIZE, xSize, UNIT_SIZE);
        }
        guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * units, 0, HEADER_SIZE + UNIT_SIZE, xSize, FOOTER_SIZE);
        guiGraphics.blit(TEXTURE, guiLeft + 10, guiTop + HEADER_SIZE + 6 - 2, xSize, 0, 12, 12);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int textX = width / 2 - 48;
        int textY = guiTop + 25;
        int crownX = width / 2 - 6;
        int crownY = guiTop + 22;
        int numbersX = 65;
        if (!playerList.isEmpty()) {
            playerList.render(guiGraphics, mouseX, mouseY, delta);
        }

        if(recruitsTeam != null){
            int members = recruitsTeam.players + recruitsTeam.npcs;
            String players = "" + recruitsTeam.players;
            String npcs = "" + recruitsTeam.npcs;

            if(recruitsTeam.maxNPCs > 0) npcs = npcs + "/" + recruitsTeam.maxNPCs;
            if(recruitsTeam.maxPlayers > 0) players = players + "/" + recruitsTeam.maxPlayers;

            guiGraphics.drawString(font, this.getTitle().getString(), width / 2F - font.width(getTitle()) / 2F, guiTop + 5, 0xFF000000 | ChatFormatting.getById(recruitsTeam.getTeamColor()).getColor(), false);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShaderTexture(0, LEADER_CROWN);
            guiGraphics.blit(LEADER_CROWN, crownX, crownY, 0, 0, 16, 16, 16, 16);

            guiGraphics.drawString(font, LEADER_TEXT.getString(), textX, textY, 4210752, false);

            guiGraphics.drawString(font, MEMBERS_TEXT.getString(), textX, textY + 25, 4210752, false);
            guiGraphics.drawString(font, "" + members, textX + numbersX, textY + 25, 4210752, false);

            guiGraphics.drawString(font, PLAYERS_TEXT.getString(), textX, textY + 40, 4210752, false);
            guiGraphics.drawString(font, players, textX + numbersX, textY + 40, 4210752, false);

            guiGraphics.drawString(font, NPCS_TEXT.getString(), textX, textY + 55, 4210752, false);
            guiGraphics.drawString(font, npcs, textX + numbersX, textY + 55, 4210752, false);
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
            name = recruitsTeam.getTeamDisplayName();
        }
        return Component.literal(name);
    }
}
