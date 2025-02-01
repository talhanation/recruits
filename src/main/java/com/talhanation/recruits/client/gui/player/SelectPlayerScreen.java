package com.talhanation.recruits.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.network.MessageToServerRequestUpdatePlayerList;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.function.Consumer;

public class SelectPlayerScreen extends ListScreenBase implements IPlayerSelection{

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_player.png");
    public static final Component TITLE = Component.translatable("gui.recruits.select_player_screen.title");
    public static final Component BUTTON_SELECT = Component.translatable("gui.recruits.select_player_screen.selectPlayer");
    public static final Component BUTTON_SELECT_TOOLTIP = Component.translatable("gui.recruits.select_player_screen.selectPlayerTooltip");
    protected static final Component BUTTON_BACK = Component.translatable("gui.recruits.button.back");
    protected static Component BUTTON_TEXT;
    protected static Component TOOLTIP_BUTTON;
    protected static final int HEADER_SIZE = 16;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 16;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 36;
    public PlayersList playerList;
    protected EditBox searchBox;
    protected String lastSearch;
    protected int units;

    protected Screen parent;
    public RecruitsPlayerInfo selected;
    private Button backButton;
    private Button actionButton;
    private final Consumer<RecruitsPlayerInfo> buttonAction;
    private final Player player;
    private final boolean includeSelf;
    private final PlayersList.FilterType filterType;

    private int gapTop;
    private int gapBottom;

    public SelectPlayerScreen(Screen parent, Player player, Component title, Component buttonText, Component buttonTooltip, boolean includeSelf, PlayersList.FilterType filterType, Consumer<RecruitsPlayerInfo> buttonAction){
        super(title,236,0);
        this.parent = parent;
        this.buttonAction = buttonAction;
        this.player = player;
        this.includeSelf = includeSelf;
        this.filterType = filterType;
        BUTTON_TEXT = buttonText;
        TOOLTIP_BUTTON = buttonTooltip;
    }


    @Override
    protected void init() {
        super.init();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdatePlayerList());

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
            playerList = new PlayersList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT, this, filterType, player, includeSelf);
        }
        String string = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE, 220, SEARCH_HEIGHT, Component.literal("SEARCH_HINT"));
        searchBox.setMaxLength(16);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(string);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(playerList);

        this.setInitialFocus(searchBox);

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;

        backButton = new Button(guiLeft + 129, buttonY, 100, 20, BUTTON_BACK,
                button -> {
                    minecraft.setScreen(parent);
         });

        actionButton = new Button(guiLeft + 7, buttonY, 100, 20, BUTTON_TEXT,
                button -> {
                buttonAction.accept(selected);
        });
        actionButton.active = false;

        addRenderableWidget(backButton);
        addRenderableWidget(actionButton);
    }

    @Override
    public void tick() {
        super.tick();
        if(searchBox != null){
            searchBox.tick();
        }
        if(playerList != null){
            playerList.tick();
        }
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.playerList.setFocused(null);
        this.actionButton.active = false;

        return flag;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
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
        font.draw(poseStack, this.getTitle(), width / 2 - font.width(TITLE) / 2, guiTop + 5, 4210752);

        if (!playerList.isEmpty()) {
            playerList.render(poseStack, mouseX, mouseY, delta);
        } else if (!searchBox.getValue().isEmpty()) {
            drawCenteredString(poseStack, font, "EMPTY_SEARCH", width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
        }
        if (!searchBox.isFocused() && searchBox.getValue().isEmpty()) {
            drawString(poseStack, font, "", searchBox.x, searchBox.y, -1);
        }
        searchBox.render(poseStack, mouseX, mouseY, delta);
    }

    private void checkSearchStringUpdate(String string) {
        if (!(string = string.toLowerCase(Locale.ROOT)).equals(lastSearch)) {
            playerList.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        if(playerList != null) playerList.mouseClicked(x,y,z);
        boolean flag = super.mouseClicked(x, y, z);
        if(this.playerList.getFocused() != null){
            this.selected = this.playerList.getFocused().getPlayerInfo();

            this.actionButton.active = true;
        }

        return flag;
    }

    @Override
    public Component getTitle() {
        return title;
    }
    @Override
    public RecruitsPlayerInfo getSelected() {
        return selected;
    }
    @Override
    public ListScreenListBase<RecruitsPlayerEntry> getPlayerList() {
        return playerList;
    }
}
