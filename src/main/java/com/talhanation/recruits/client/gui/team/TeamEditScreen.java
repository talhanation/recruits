package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.ColorChatFormattingSelectionDropdownMatrix;
import com.talhanation.recruits.client.gui.widgets.ColorSelectionDropdownMatrix;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.inventory.TeamEditMenu;
import com.talhanation.recruits.network.MessageCreateTeam;
import com.talhanation.recruits.network.MessageSaveTeamSettings;
import com.talhanation.recruits.network.MessageToServerRequestUpdatePlayerCurrencyCount;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import com.talhanation.recruits.world.RecruitsTeamManager;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static com.talhanation.recruits.client.gui.team.TeamInspectionScreen.LEADER_CROWN;

public class TeamEditScreen extends ScreenBase<TeamEditMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/team/team_create_gui.png");
    private static final Component EDIT = Component.translatable("gui.recruits.team.edit");
    private static final Component BACK = Component.translatable("gui.recruits.button.back");
    private static final Component SAVE = Component.translatable("gui.recruits.button.save");
    private static final Component CREATE = Component.translatable("gui.recruits.team_creation.create_team");
    private static final Component TEAM_NAME = Component.translatable("gui.recruits.team.team_name");
    private static final Component LEADER = Component.translatable("gui.recruits.team.leader");
    private static final Component TEAM_COLOR = Component.translatable("gui.recruits.team.team_color");
    private static final Component UNITS_COLOR = Component.translatable("gui.recruits.team.units_color");
    private static final Component MAX_RECRUITS = Component.translatable("gui.recruits.team.max_recruits");
    private static final Component SELECT_LEADER_TOOLTIP = Component.translatable("gui.recruits.team.select_leader_tooltip");
    public int maxRecruitsPerPlayer;
    public static ItemStack currency;
    public static int creationPrice;
    private ColorChatFormattingSelectionDropdownMatrix teamColorDropdownMatrix;
    private ColorSelectionDropdownMatrix unitColorDropdownMatrix;
    private final Player player;
    @Nullable
    public static RecruitsTeam recruitsTeam;
    private Screen parent;
    private ChatFormatting teamColor;
    private Color unitColor;
    private BannerRenderer bannerRenderer;
    private EditBox textFieldTeamName;
    private SelectedPlayerWidget leaderWidget;
    public static RecruitsPlayerInfo leaderInfo;
    private ItemStack banner;
    private Button saveButton;
    public static int maxRecruitsPerPlayerConfigSetting;
    private int guiLeft;
    private int guiTop;
    private int totalCost;
    public static int playerCurrencyCount;
    private boolean hasChanges;
    private String teamNameSavedValue;
    public static final ArrayList<ChatFormatting> teamColors = new ArrayList<>(
            Arrays.asList(
                    ChatFormatting.BLACK,
                    ChatFormatting.DARK_BLUE,
                    ChatFormatting.DARK_GREEN,
                    ChatFormatting.DARK_AQUA,
                    ChatFormatting.DARK_RED,
                    ChatFormatting.DARK_PURPLE,
                    ChatFormatting.GOLD,
                    ChatFormatting.GRAY,
                    ChatFormatting.DARK_GRAY,
                    ChatFormatting.BLUE,
                    ChatFormatting.GREEN,
                    ChatFormatting.AQUA,
                    ChatFormatting.RED,
                    ChatFormatting.LIGHT_PURPLE,
                    ChatFormatting.YELLOW,
                    ChatFormatting.WHITE
            )
    );

    public static final ArrayList<Component> teamColorsNames = new ArrayList<>(
            Arrays.asList(
                    Component.translatable("gui.recruits.team_color.black"),
                    Component.translatable("gui.recruits.team_color.dark_blue"),
                    Component.translatable("gui.recruits.team_color.dark_green"),
                    Component.translatable("gui.recruits.team_color.dark_aqua"),
                    Component.translatable("gui.recruits.team_color.dark_red"),
                    Component.translatable("gui.recruits.team_color.dark_purple"),
                    Component.translatable("gui.recruits.team_color.gold"),
                    Component.translatable("gui.recruits.team_color.gray"),
                    Component.translatable("gui.recruits.team_color.dark_gray"),
                    Component.translatable("gui.recruits.team_color.blue"),
                    Component.translatable("gui.recruits.team_color.green"),
                    Component.translatable("gui.recruits.team_color.aqua"),
                    Component.translatable("gui.recruits.team_color.red"),
                    Component.translatable("gui.recruits.team_color.light_purple"),
                    Component.translatable("gui.recruits.team_color.yellow"),
                    Component.translatable("gui.recruits.team_color.white")
            )
    );

    //list index are important
    public static final ArrayList<Color> unitColors = new ArrayList<>(
            Arrays.asList(
                    new Color(255, 255, 255), // White
                    new Color(0, 0, 0),       // Black
                    new Color(211, 211, 211), // Light Gray
                    new Color(128, 128, 128), // Gray
                    new Color(64, 64, 64),    // Dark Gray
                    new Color(51, 123, 154), // Light Blue
                    new Color(51, 92, 154),     // Blue
                    new Color(21, 37, 62),     // Dark Blue
                    new Color(38, 156, 8), // Light Green
                    new Color(0, 128, 0),     // Green
                    new Color(14, 57, 3),     // Dark Green
                    new Color(185, 21, 21), // Light Red (Pink)
                    new Color(148, 16, 16),     // Red
                    new Color(74, 8, 8),     // Dark Red
                    new Color(140, 76, 24), // Light Brown
                    new Color(94, 59, 29),    // Brown
                    new Color(63, 39, 19),    // Dark Brown
                    new Color(0, 150, 150), // Light Cyan
                    new Color(23, 68, 68),   // Cyan
                    new Color(11, 34, 34),   // Dark Cyan
                    new Color(237, 202, 18),   // Yellow
                    new Color(229, 106, 17),   // Orange
                    new Color(255, 0, 255),   // Magenta
                    new Color(87, 2, 101),  // Purple
                    new Color(221, 154, 25)    // Gold
            )
    );

    public static final ArrayList<Component> unitColorsNames = new ArrayList<>(
            Arrays.asList(
                    Component.translatable("gui.recruits.team_color.white"),
                    Component.translatable("gui.recruits.team_color.black"),
                    Component.translatable("gui.recruits.team_color.light_gray"),
                    Component.translatable("gui.recruits.team_color.gray"),
                    Component.translatable("gui.recruits.team_color.dark_gray"),
                    Component.translatable("gui.recruits.team_color.light_blue"),
                    Component.translatable("gui.recruits.team_color.blue"),
                    Component.translatable("gui.recruits.team_color.dark_blue"),
                    Component.translatable("gui.recruits.team_color.light_green"),
                    Component.translatable("gui.recruits.team_color.green"),
                    Component.translatable("gui.recruits.team_color.dark_green"),
                    Component.translatable("gui.recruits.team_color.light_red"),
                    Component.translatable("gui.recruits.team_color.red"),
                    Component.translatable("gui.recruits.team_color.dark_red"),
                    Component.translatable("gui.recruits.team_color.light_brown"),
                    Component.translatable("gui.recruits.team_color.brown"),
                    Component.translatable("gui.recruits.team_color.dark_brown"),
                    Component.translatable("gui.recruits.team_color.light_cyan"),
                    Component.translatable("gui.recruits.team_color.cyan"),
                    Component.translatable("gui.recruits.team_color.dark_cyan"),
                    Component.translatable("gui.recruits.team_color.yellow"),
                    Component.translatable("gui.recruits.team_color.orange"),
                    Component.translatable("gui.recruits.team_color.magenta"),
                    Component.translatable("gui.recruits.team_color.purple"),
                    Component.translatable("gui.recruits.team_color.gold")
            )
    );

    public TeamEditScreen(TeamEditMenu container, Inventory playerInventory, Component title) {
        super(TEXTURE, container, playerInventory, Component.literal(""));
        this.imageHeight = 240;
        this.imageWidth = 222;
        this.player = playerInventory.player;
        this.menu.setScreen(this);
        this.teamNameSavedValue = "";
    }

    @Override
    protected void init() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdatePlayerCurrencyCount());
        this.guiLeft = (width - this.imageWidth) / 2;
        this.guiTop = (height - this.imageHeight) / 2;
        postInit = true;
        super.init();
    }

    public void postInit(){
        if(recruitsTeam != null){
            this.teamColor = ChatFormatting.getById(recruitsTeam.getTeamColor());
            this.unitColor = unitColors.get(recruitsTeam.getUnitColor());
            if(leaderInfo == null){
                leaderInfo = new RecruitsPlayerInfo(recruitsTeam.getTeamLeaderUUID(), recruitsTeam.getTeamLeaderName(), recruitsTeam);
            }

            maxRecruitsPerPlayer = recruitsTeam.getMaxNPCsPerPlayer();
            if(maxRecruitsPerPlayer == -1) maxRecruitsPerPlayer = maxRecruitsPerPlayerConfigSetting;

            this.parent = new TeamInspectionScreen(new TeamMainScreen(player), player);
            this.bannerRenderer = new BannerRenderer(recruitsTeam);
        }
        else {
            this.teamColor = teamColors.get(15);
            this.unitColor = unitColors.get(0);
            if(leaderInfo == null) {
                leaderInfo = new RecruitsPlayerInfo(player.getUUID(), player.getName().getString());
            }
            maxRecruitsPerPlayer = maxRecruitsPerPlayerConfigSetting;
            this.parent = new TeamMainScreen(player);
            this.bannerRenderer = new BannerRenderer(null);
        }


        setWidgets();
    }

    public void setParent(Screen parent) {
        this.parent = parent;
    }
    public static boolean postInit;
    @Override
    public void containerTick() {
        super.containerTick();

        if(textFieldTeamName != null) textFieldTeamName.tick();

        if(postInit) {
            this.postInit();
            postInit = false;
        }

        if(saveButton != null){
            saveButton.active = recruitsTeam != null ? checkEditCondition(): checkCreationCondition();
        }


    }

    private void setWidgets() {
        clearWidgets();
        String teamName = recruitsTeam != null ? recruitsTeam.getTeamDisplayName() : this.teamNameSavedValue;

        int textsX = 46;
        int gap = 3;
        int widgetsX = 107;
        int widgetsY = 225;
        this.banner = menu.getBanner();

        textFieldTeamName = new EditBox(font, guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 0, 110, 20, Component.literal(teamName));
        textFieldTeamName.setTextColor(-1);
        textFieldTeamName.setTextColorUneditable(-1);
        textFieldTeamName.setBordered(true);
        textFieldTeamName.setMaxLength(32);
        textFieldTeamName.setValue(teamName);
        textFieldTeamName.setResponder(this::onTextInput);

        addWidget(textFieldTeamName);
        this.setInitialFocus(textFieldTeamName);

        addRenderableOnly(new BlackShowingTextField(guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 4, 110, 20, 45, 0, getMaxRecruitsPerPlayerString()));

        if (leaderInfo != null) {
            this.leaderWidget = new SelectedPlayerWidget(font, guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 1, 110, 20, Component.literal("x"), // Button label
                    () -> {
                        leaderInfo = null;
                        this.leaderWidget.setPlayer(null, null);

                        this.setWidgets();
                    }
            );

            this.leaderWidget.setPlayer(leaderInfo.getUUID(), leaderInfo.getName());
            this.leaderWidget.setButtonActive(recruitsTeam != null);
            this.leaderWidget.setButtonVisible(recruitsTeam != null);
            addRenderableWidget(this.leaderWidget);
        } else {
            Button selectPlayerButton = addRenderableWidget(new ExtendedButton(guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 1, 110, 20, SelectPlayerScreen.TITLE,
                button -> {
                    Screen parent = recruitsTeam == null ? new TeamMainScreen(player) : new TeamInspectionScreen(new TeamMainScreen(player), player);
                    minecraft.setScreen(new SelectPlayerScreen(parent, player, SelectPlayerScreen.TITLE, SelectPlayerScreen.BUTTON_SELECT, SelectPlayerScreen.BUTTON_SELECT_TOOLTIP, false, PlayersList.FilterType.SAME_TEAM,
                        (playerInfo) -> {
                            leaderInfo = playerInfo;
                            TeamEvents.openTeamEditScreen(player);
                            setWidgets();
                        }
                    ));
                }
            ));
            selectPlayerButton.setTooltip(Tooltip.create(SELECT_LEADER_TOOLTIP));
        }

        teamColorDropdownMatrix = new ColorChatFormattingSelectionDropdownMatrix(this, guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 2, 110, 20,
                teamColors,
                this::setTeamColor
        );
        addRenderableWidget(teamColorDropdownMatrix);


        unitColorDropdownMatrix = new ColorSelectionDropdownMatrix(this, guiLeft + widgetsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 3, 110, 20,
                unitColors,
                this::setUnitColor
        );
        addRenderableWidget(unitColorDropdownMatrix);

        addRenderableWidget(new ExtendedButton(guiLeft + widgetsX + 20, guiTop + imageHeight - widgetsY + (20 + gap ) * 4, 20, 20,Component.literal("-"),
            (button)-> {
                if(hasShiftDown()){
                    maxRecruitsPerPlayer -= 5;
                }
                else
                    maxRecruitsPerPlayer--;

                if(maxRecruitsPerPlayer < 0) maxRecruitsPerPlayer = 0;

                setWidgets();
            }));

        addRenderableWidget(new ExtendedButton(guiLeft + widgetsX + 90, guiTop + imageHeight - widgetsY + (20 + gap ) * 4, 20, 20,Component.literal("+"),
        (button)-> {
            if(hasShiftDown()){
                maxRecruitsPerPlayer += 5;
            }
            else
                maxRecruitsPerPlayer++;

            if(maxRecruitsPerPlayer > maxRecruitsPerPlayerConfigSetting) maxRecruitsPerPlayer = maxRecruitsPerPlayerConfigSetting;

            setWidgets();
        }));

        this.banner = menu.getBanner();
        if(recruitsTeam == null){
            saveButton = new ExtendedButton(guiLeft + 30, guiTop + imageHeight - 102, 162, 20, CREATE,
                btn -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCreateTeam(this.getCorrectFormat(textFieldTeamName.getValue().strip()), banner, teamColor, unitColors.indexOf(unitColor)));
                    leaderInfo = null;
                    minecraft.setScreen(new TeamInspectionScreen(new TeamMainScreen(player), player));
                }
            );

            saveButton.active = checkCreationCondition();
            addRenderableWidget(saveButton);
        }
        else {
            addRenderableWidget(new ExtendedButton(guiLeft + 117, guiTop + imageHeight - 102, 75, 20, BACK,
                btn -> {
                    leaderInfo = null;
                    minecraft.setScreen(parent);
                }
            ));

            saveButton = new ExtendedButton(guiLeft + 30, guiTop + imageHeight - 102, 75, 20, SAVE,
                btn -> {
                    recruitsTeam.setTeamLeaderID(leaderInfo.getUUID());
                    recruitsTeam.setTeamLeaderName(leaderInfo.getName());
                    recruitsTeam.setTeamDisplayName(textFieldTeamName.getValue());
                    recruitsTeam.setTeamColor(teamColor.getId());
                    if(banner != null && !banner.isEmpty()){
                        recruitsTeam.setBanner(banner.serializeNBT());
                    }
                    recruitsTeam.setUnitColor((byte) unitColors.indexOf(unitColor));
                    recruitsTeam.setMaxNPCsPerPlayer(maxRecruitsPerPlayer);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveTeamSettings(recruitsTeam, totalCost));

                    minecraft.setScreen(new TeamInspectionScreen(new TeamMainScreen(player), player));
                }
            );

            saveButton.active = checkEditCondition();
            addRenderableWidget(saveButton);
        }
        calculateCost();

        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 0, 60, 20, TEAM_NAME));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 1, 60, 20, LEADER));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 2, 60, 20, TEAM_COLOR));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 3, 60, 20, UNITS_COLOR));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + imageHeight - widgetsY + (20 + gap ) * 4, 60, 20, MAX_RECRUITS));
    }

    private void calculateCost() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdatePlayerCurrencyCount());
        if (recruitsTeam != null) {
            hasChanges = false;
            totalCost = 0;

            if (teamColor != null && recruitsTeam.getTeamColor() != teamColor.getId()) {
                totalCost += creationPrice;
                hasChanges = true;
            }

            if (recruitsTeam.getUnitColor() != unitColors.indexOf(unitColor)) {
                totalCost += creationPrice;
                hasChanges = true;
            }

            if (leaderInfo != null && !recruitsTeam.getTeamLeaderUUID().equals(leaderInfo.getUUID())) {
                totalCost += creationPrice;
                hasChanges = true;
            }

            if (textFieldTeamName != null && !recruitsTeam.getTeamDisplayName().equals(textFieldTeamName.getValue())) {
                totalCost += creationPrice;
                hasChanges = true;
            }

            if (recruitsTeam.getMaxNPCsPerPlayer() != maxRecruitsPerPlayer) {
                totalCost += creationPrice;
                hasChanges = true;
            }

            if(banner != null && !banner.isEmpty() && !recruitsTeam.getBanner().equals(banner.serializeNBT())){
                totalCost += creationPrice;
                hasChanges = true;
            }
        }
        else {
            totalCost = creationPrice;
        }

        if(currency != null) currency.setCount(totalCost);
    }

    private void setUnitColor(Color color) {
        this.unitColor = color;
        setWidgets();
    }

    private void setTeamColor(ChatFormatting selected) {
        this.teamColor = selected;
        setWidgets();
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(teamColorDropdownMatrix != null) teamColorDropdownMatrix.onMouseMove(x,y);
        if(unitColorDropdownMatrix != null) unitColorDropdownMatrix.onMouseMove(x,y);
        super.mouseMoved(x, y);
    }


    @Override
    public boolean mouseClicked(double x, double y, int p_94697_) {
        if(teamColorDropdownMatrix != null) teamColorDropdownMatrix.onMouseClicked(x,y);
        if(unitColorDropdownMatrix != null) unitColorDropdownMatrix.onMouseClicked(x,y);

        return super.mouseClicked(x, y, p_94697_);
    }

    int x1 = 15;
    int y1 = 61;
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if(bannerRenderer != null) bannerRenderer.renderBanner(guiGraphics, this.guiLeft + x1, guiTop + y1, this.width, this.height, 50);
        renderForeground(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float p_97788_, int p_97789_, int p_97790_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);
    }

    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int crownX = width / 2 - 23;
        int crownY = guiTop + 41;

        int currencyX = width / 2 - 28;
        int currencyY = guiTop + 140;

        if(recruitsTeam == null){
            currencyX = width / 2 + 63;
        }

        guiGraphics.drawString(font, getTitle(), guiLeft + imageWidth / 2 - font.width(getTitle()) / 2, guiTop + 5, FONT_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, LEADER_CROWN);
        guiGraphics.blit(LEADER_CROWN, crownX, crownY, 0, 0, 16, 16, 16, 16);

        if(totalCost > 0 && currency != null){
            guiGraphics.renderFakeItem(currency, currencyX, currencyY);
            guiGraphics.renderItemDecorations(font, currency, currencyX, currencyY);
        }

        if(textFieldTeamName != null)textFieldTeamName.render(guiGraphics, mouseX, mouseY, delta);
    }

    public Component getMaxRecruitsPerPlayerString() {
        return Component.translatable("gui.recruits.two_values_with_slash", maxRecruitsPerPlayer, maxRecruitsPerPlayerConfigSetting);
    }

    public ChatFormatting getSelectedTeamColor() {
        return this.teamColor;
    }

    public Color getSelectedUnitColor() {
        return this.unitColor;
    }
    public int getSelectedUnitColorNameIndex() {
        return unitColors.indexOf(this.unitColor);
    }

    public int getSelectedTeamColorNameIndex() {
        return teamColor.getId();
    }

    private String getCorrectFormat(String input) {
        input = input.replaceAll(" ", "");
        input = input.replaceAll("[^a-zA-Z0-9\\s]+", "");

        return input;
    }

    private boolean checkCreationCondition(){
        boolean nameLength = this.textFieldTeamName != null && this.textFieldTeamName.getValue().length() >= 3 && this.textFieldTeamName.getValue().length() <= 32;
        boolean sufficientEmeralds =  playerCurrencyCount >= creationPrice || player.isCreative();
        boolean bannerNotEmpty = this.banner != null && !this.banner.isEmpty() && !RecruitsTeamManager.isBannerBlank(banner);
        return bannerNotEmpty && nameLength && leaderInfo != null && sufficientEmeralds;
    }

    private boolean checkEditCondition(){
        boolean nameLength = this.textFieldTeamName != null && this.textFieldTeamName.getValue().length() >= 3 && this.textFieldTeamName.getValue().length() <= 32;
        boolean sufficientEmeralds = playerCurrencyCount >= totalCost || player.isCreative();

        return nameLength && leaderInfo != null && sufficientEmeralds && hasChanges;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    public void onBannerPlaced(){
        if(menu.getBanner() != null && bannerRenderer != null){
            if(menu.getBanner().isEmpty()){
                if(recruitsTeam != null) bannerRenderer.setBannerItem(ItemStack.of(recruitsTeam.getBanner()));
                this.banner = null;
            }
            else{
                this.banner = menu.getBanner();
                bannerRenderer.setBannerItem(menu.getBanner());
            }
        }
        calculateCost();
        if(saveButton != null) saveButton.active = checkEditCondition();
    }

    private void onTextInput(String string) {
        if(recruitsTeam == null){
            saveButton.active = checkCreationCondition();
        }
        else{
            calculateCost();
            saveButton.active = checkEditCondition();
        }
        this.teamNameSavedValue = string;
    }

    public void onPlayerInventoryChanged(){

         Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdatePlayerCurrencyCount());
        if(saveButton != null) saveButton.active = recruitsTeam != null ? checkEditCondition(): checkCreationCondition();
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if(this.textFieldTeamName == null) super.keyPressed(key, a, b);

        setFocused(textFieldTeamName);

        return textFieldTeamName.keyPressed(key, a, b) || textFieldTeamName.canConsumeInput() || super.keyPressed(key, a, b);
    }

    public void removed() {
        super.removed();
        recruitsTeam = null;
    }
}
