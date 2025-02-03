package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.RecruitsToastManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.network.MessageSendJoinRequestTeam;
import com.talhanation.recruits.network.MessageToServerRequestUpdateTeamList;
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
import static com.talhanation.recruits.client.events.RecruitsToastManager.*;


@OnlyIn(Dist.CLIENT)
public class RecruitsTeamListScreen extends ListScreenBase {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_player.png");
    protected static final Component TITLE = Component.translatable("gui.recruits.team_creation.teams_list");
    protected static final Component JOIN_BUTTON = Component.translatable("gui.recruits.button.join");
    protected static final Component BACK_BUTTON = Component.translatable("gui.recruits.button.back");
    protected static final int HEADER_SIZE = 16;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 16;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 36;

    protected RecruitsTeamList teamList;
    protected EditBox searchBox;
    protected String lastSearch;
    protected int units;

    protected Screen parent;
    private RecruitsTeam selected;
    private Button backButton;
    private Button sendJoinRequestButton;

    private int gapTop;
    private int gapBottom;

    public RecruitsTeamListScreen(Screen parent){
        super(TITLE,236,0);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateTeamList());

        gapTop = (int) (this.height * 0.1);
        gapBottom = (int) (this.height * 0.1);

        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);

        if (teamList != null) {
            teamList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            teamList = new RecruitsTeamList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT, this);
        }
        String string = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE, 220, SEARCH_HEIGHT, Component.literal(""));
        searchBox.setMaxLength(16);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(string);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(teamList);

        this.setInitialFocus(searchBox);

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;

        backButton = new ExtendedButton(guiLeft + 129, buttonY, 100, 20, BACK_BUTTON,
                button -> {
                    minecraft.setScreen(parent);
                });

        addRenderableWidget(backButton);

        sendJoinRequestButton = new ExtendedButton(guiLeft + 7, buttonY, 100, 20, JOIN_BUTTON,
                button -> {
                    RecruitsToastManager.setTeamToastForPlayer(RecruitsToastManager.Images.LETTER, TOAST_SENT_JOIN_REQUEST_TITLE, TOAST_TO(selected.getTeamDisplayName()), selected);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSendJoinRequestTeam(parent.getMinecraft().player.getUUID(), selected.getStringID()));
                });
        sendJoinRequestButton.active = false;

        addRenderableWidget(sendJoinRequestButton);
    }

    @Override
    public void tick() {
        super.tick();
        if(searchBox != null){
            searchBox.tick();
        }
        if(teamList != null){
            teamList.tick();
        }
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.teamList.setFocused(null);
        this.sendJoinRequestButton.active = false;

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

        if (!teamList.isEmpty()) {
            teamList.render(guiGraphics, mouseX, mouseY, delta);
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
            teamList.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        if(teamList != null) teamList.mouseClicked(x,y,z);
        boolean flag = super.mouseClicked(x, y, z);
        if(this.teamList.getFocused() != null){
            this.selected = this.teamList.getFocused().getTeamInfo();
            this.sendJoinRequestButton.active = minecraft.player.getTeam() == null;
        }

        return flag;
    }


    public RecruitsTeam getSelected(){
        return this.selected;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

}

