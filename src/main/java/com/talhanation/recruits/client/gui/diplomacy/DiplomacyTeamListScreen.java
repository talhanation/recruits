package com.talhanation.recruits.client.gui.diplomacy;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.network.MessageToServerRequestUpdateDiplomacyList;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.Locale;
@OnlyIn(Dist.CLIENT)
public class DiplomacyTeamListScreen extends ListScreenBase {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_with_filter.png");
    protected static final Component TITLE = Component.translatable("gui.recruits.diplomacy.teams_list");
    protected static final Component SET_STANCE = Component.translatable("gui.recruits.button.setRelation");
    protected static final Component SHOW_STANCE = Component.translatable("gui.recruits.button.showRelation");
    protected static final Component TOAST_SEND_JOIN_REQUEST_TITLE = Component.translatable("gui.recruits.toast.sendJoinRequestTitle");
    protected static final Component BACK_BUTTON = Component.translatable("gui.recruits.button.back");
    protected static final int HEADER_SIZE = 32;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 16;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 36;

    protected DiplomacyTeamList list;
    protected EditBox searchBox;
    protected String lastSearch;
    protected int units;

    protected Screen parent;
    private RecruitsTeam selected;
    protected RecruitsTeam ownTeam;
    private Button backButton;
    private Button setStanceButton;
    private final boolean isLeader;
    private int gapBottom;
    private int gapTop;

    private Button alliesButton;
    private Button neutralsButton;
    private Button enemiesButton;

    private DiplomacyTeamList.DiplomacyFilter diplomacyFilter;

    public DiplomacyTeamListScreen(Screen parent, boolean isLeader) {
        super(TITLE, 236, 0);
        this.parent = parent;
        this.isLeader = isLeader;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateDiplomacyList());

        gapTop = (int) (this.height * 0.1);
        gapBottom = (int) (this.height * 0.1);

        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);
        ySize = HEADER_SIZE + units * UNIT_SIZE + FOOTER_SIZE;

        if(diplomacyFilter == null){
            this.diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ALL;
        }

        if (list != null) {

            list.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            list = new DiplomacyTeamList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT, this);
        }

        String string = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE - 4, 156, SEARCH_HEIGHT + 4, Component.literal(""));
        searchBox.setMaxLength(16);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(string);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(list);

        this.setInitialFocus(searchBox);
        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;

        backButton = new ExtendedButton(guiLeft + 129, buttonY, 100, 20, BACK_BUTTON,
                button -> {
                    minecraft.setScreen(parent);
                });

        addRenderableWidget(backButton);

        setStanceButton = new ExtendedButton(guiLeft + 7, buttonY, 100, 20, this.isLeader ? SET_STANCE : SHOW_STANCE,
                button -> {
                     minecraft.setScreen(new DiplomacyEditScreen(this, ownTeam, selected, list.getRelation(ownTeam.getStringID(), selected.getStringID()), list.getRelation(selected.getStringID(), ownTeam.getStringID()), isLeader));
                     this.selected = null;
                });
        setStanceButton.active = ownTeam != null && ownTeam.getTeamLeaderUUID().equals(this.minecraft.player.getUUID());

        addRenderableWidget(setStanceButton);

        int buttonX = 165;
        int buttonY2 = 28;
        alliesButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ALLY, guiLeft + buttonX, guiTop + buttonY2, 21, 21, Component.literal(""),
                button -> {
                    if(diplomacyFilter != DiplomacyTeamList.DiplomacyFilter.ALLIES){
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ALLIES;
                    }
                    else{
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ALL;
                    }
                    listUpdateFilter();;
                });

        neutralsButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL,guiLeft + buttonX + 21, guiTop + buttonY2, 21, 21, Component.literal(""),
                button -> {
                    if(diplomacyFilter != DiplomacyTeamList.DiplomacyFilter.NEUTRALS){
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.NEUTRALS;
                    }
                    else{
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ALL;
                    }
                    listUpdateFilter();
                });

        enemiesButton = new RecruitsDiplomacyButton(RecruitsDiplomacyManager.DiplomacyStatus.ENEMY,guiLeft + buttonX + 21 + 21, guiTop + buttonY2, 21, 21, Component.literal(""),
                button -> {
                    if(diplomacyFilter != DiplomacyTeamList.DiplomacyFilter.ENEMIES){
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ENEMIES;
                    }
                    else{
                        diplomacyFilter = DiplomacyTeamList.DiplomacyFilter.ALL;
                    }
                    listUpdateFilter();
                });
        listUpdateFilter();
        addRenderableWidget(alliesButton);
        addRenderableWidget(enemiesButton);
        addRenderableWidget(neutralsButton);
    }

    private void listUpdateFilter() {
        alliesButton.active =  diplomacyFilter == DiplomacyTeamList.DiplomacyFilter.ALLIES;
        enemiesButton.active = diplomacyFilter == DiplomacyTeamList.DiplomacyFilter.ENEMIES;
        neutralsButton.active = diplomacyFilter == DiplomacyTeamList.DiplomacyFilter.NEUTRALS;

        list.diplomacyFilter = diplomacyFilter;
        list.hasUpdated = false;
    }
    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) {
            searchBox.tick();
        }
        if (list != null) {
            list.tick();
        }
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.list.setFocused(null);
        this.setStanceButton.active = false;

        return flag;
    }

    @Override
    public void onClose() {
        super.onClose();
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
        guiGraphics.drawString(font, this.getTitle(), width / 2 - font.width(TITLE) / 2, guiTop + 5, 4210752, false);

        if (!list.isEmpty()) {
            list.render(guiGraphics, mouseX, mouseY, delta);
        } else if (!searchBox.getValue().isEmpty()) {
            guiGraphics.drawCenteredString(font, "EMPTY_SEARCH", width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
        }
        if (!searchBox.isFocused() && searchBox.getValue().isEmpty()) {
            guiGraphics.drawString(font, "", searchBox.getX(), searchBox.getY(), -1, false);
        }
        searchBox.render(guiGraphics, mouseX, mouseY, delta);
    }

    private void checkSearchStringUpdate(String string) {
        if (!(string = string.toLowerCase(Locale.ROOT)).equals(lastSearch)) {
            list.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        if (list != null) list.mouseClicked(x, y, z);
        boolean flag = super.mouseClicked(x, y, z);
        if (this.list.getFocused() != null) {
            this.selected = this.list.getFocused().team;
            this.setStanceButton.active = true;
        }

        return flag;
    }


    public RecruitsTeam getSelected() {
        return this.selected;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }


}