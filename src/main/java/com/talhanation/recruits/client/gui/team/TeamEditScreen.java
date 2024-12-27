package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.ColorChatFormattingSelectionDropdownMatrix;
import com.talhanation.recruits.client.gui.widgets.ColorSelectionDropdownMatrix;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.network.MessageCreateTeam;
import com.talhanation.recruits.network.MessageSaveTeamSettings;
import com.talhanation.recruits.network.MessageToServerRequestUpdateTeamEdit;
import com.talhanation.recruits.network.MessageToServerRequestUpdateTeamInspaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static com.talhanation.recruits.client.gui.team.TeamInspectionScreen.LEADER_CROWN;

public class TeamEditScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/team/team_create_gui.png");
    private static final Component EDIT = new TranslatableComponent("gui.recruits.team.edit");
    private static final Component BACK = new TranslatableComponent("gui.recruits.button.back");
    private static final Component SAVE = new TranslatableComponent("gui.recruits.button.save");
    private static final Component CREATE = new TranslatableComponent("gui.recruits.team_creation.create_team");

    private static final Component TEAM_NAME = new TranslatableComponent("gui.recruits.team.team_name");
    private static final Component LEADER = new TranslatableComponent("gui.recruits.team.leader");
    private static final Component TEAM_COLOR = new TranslatableComponent("gui.recruits.team.team_color");
    private static final Component UNITS_COLOR = new TranslatableComponent("gui.recruits.team.units_color");
    private static final Component MAX_RECRUITS = new TranslatableComponent("gui.recruits.team.max_recruits");
    private static final Component SELECT_LEADER_TOOLTIP = new TranslatableComponent("gui.recruits.team.select_leader_tooltip");
    public static int maxRecruitsPerPlayer;
    public static ItemStack currency;
    public static int creationPrice;
    private ColorChatFormattingSelectionDropdownMatrix teamColorDropdownMatrix;
    private ColorSelectionDropdownMatrix unitColorDropdownMatrix;
    private final Player player;
    @Nullable
    private final RecruitsTeam recruitsTeam;
    private final Screen parent;
    private ChatFormatting teamColor;
    private Color unitColor;
    private BannerRenderer bannerRenderer;
    private EditBox textFieldTeamName;
    private SelectedPlayerWidget leaderWidget;
    private RecruitsPlayerInfo leaderInfo;
    private ItemStack banner;
    private Button saveButton;
    public static int maxRecruitsPerPlayerConfigSetting;
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
                    new TranslatableComponent("gui.recruits.team_color.black"),
                    new TranslatableComponent("gui.recruits.team_color.dark_blue"),
                    new TranslatableComponent("gui.recruits.team_color.dark_green"),
                    new TranslatableComponent("gui.recruits.team_color.dark_aqua"),
                    new TranslatableComponent("gui.recruits.team_color.dark_red"),
                    new TranslatableComponent("gui.recruits.team_color.dark_purple"),
                    new TranslatableComponent("gui.recruits.team_color.gold"),
                    new TranslatableComponent("gui.recruits.team_color.gray"),
                    new TranslatableComponent("gui.recruits.team_color.dark_gray"),
                    new TranslatableComponent("gui.recruits.team_color.blue"),
                    new TranslatableComponent("gui.recruits.team_color.green"),
                    new TranslatableComponent("gui.recruits.team_color.aqua"),
                    new TranslatableComponent("gui.recruits.team_color.red"),
                    new TranslatableComponent("gui.recruits.team_color.light_purple"),
                    new TranslatableComponent("gui.recruits.team_color.yellow"),
                    new TranslatableComponent("gui.recruits.team_color.white")
            )
    );

    //list index are important
    public static final ArrayList<Color> unitColors = new ArrayList<>(
            Arrays.asList(
                Color.WHITE,
                Color.BLACK,
                Color.LIGHT_GRAY,
                Color.GRAY,
                Color.DARK_GRAY,
                Color.BLUE.brighter(),
                Color.BLUE,
                Color.BLUE.darker(),
                Color.GREEN.brighter(),
                Color.GREEN,
                Color.GREEN.darker(),
                Color.RED.brighter(),
                Color.RED,
                Color.RED.darker(),
                new Color(196, 164, 132), //light brown
                new Color(150, 75, 0), //Brown
                new Color(92, 64, 51), // dark brown
                Color.CYAN.brighter(),
                Color.CYAN,
                Color.CYAN.darker(),
                Color.YELLOW,
                Color.ORANGE,
                Color.MAGENTA,
                new Color(160, 32, 240),//Purple
                new Color(255, 215,0)// Gold
            )
    );

    public static final ArrayList<Component> unitColorsNames = new ArrayList<>(
            Arrays.asList(
                    new TranslatableComponent("gui.recruits.team_color.white"),
                    new TranslatableComponent("gui.recruits.team_color.black"),
                    new TranslatableComponent("gui.recruits.team_color.light_gray"),
                    new TranslatableComponent("gui.recruits.team_color.gray"),
                    new TranslatableComponent("gui.recruits.team_color.dark_gray"),
                    new TranslatableComponent("gui.recruits.team_color.light_blue"),
                    new TranslatableComponent("gui.recruits.team_color.blue"),
                    new TranslatableComponent("gui.recruits.team_color.dark_blue"),
                    new TranslatableComponent("gui.recruits.team_color.light_green"),
                    new TranslatableComponent("gui.recruits.team_color.green"),
                    new TranslatableComponent("gui.recruits.team_color.dark_green"),
                    new TranslatableComponent("gui.recruits.team_color.light_red"),
                    new TranslatableComponent("gui.recruits.team_color.red"),
                    new TranslatableComponent("gui.recruits.team_color.dark_red"),
                    new TranslatableComponent("gui.recruits.team_color.light_brown"),
                    new TranslatableComponent("gui.recruits.team_color.brown"),
                    new TranslatableComponent("gui.recruits.team_color.dark_brown"),
                    new TranslatableComponent("gui.recruits.team_color.light_cyan"),
                    new TranslatableComponent("gui.recruits.team_color.cyan"),
                    new TranslatableComponent("gui.recruits.team_color.dark_cyan"),
                    new TranslatableComponent("gui.recruits.team_color.yellow"),
                    new TranslatableComponent("gui.recruits.team_color.orange"),
                    new TranslatableComponent("gui.recruits.team_color.magenta"),
                    new TranslatableComponent("gui.recruits.team_color.purple"),
                    new TranslatableComponent("gui.recruits.team_color.gold")
            )
    );

    public TeamEditScreen(Screen parent, Player player, @Nullable RecruitsTeam recruitsTeam) {
        super(recruitsTeam != null ? EDIT : CREATE, 222,240);
        this.parent = parent;
        this.player = player;
        this.recruitsTeam = recruitsTeam;
        this.bannerRenderer = new BannerRenderer(recruitsTeam);
    }

    @Override
    protected void init() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateTeamEdit());

        super.init();

        if(recruitsTeam != null){
            this.teamColor = ChatFormatting.getById(recruitsTeam.getTeamColor());
            this.unitColor = unitColors.get(recruitsTeam.getUnitColor());
            this.leaderInfo = new RecruitsPlayerInfo(recruitsTeam.getTeamLeaderUUID(), recruitsTeam.getTeamLeaderName(), recruitsTeam);
        }
        else {
            this.teamColor = teamColors.get(15);
            this.unitColor = unitColors.get(0);
            this.leaderInfo = new RecruitsPlayerInfo(player.getUUID(), player.getName().getString());
        }

        setWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if(textFieldTeamName != null) textFieldTeamName.tick();
    }

    private void setWidgets() {
        clearWidgets();
        String teamName = recruitsTeam != null ? recruitsTeam.getTeamName() : "";
        int textsX = 46;
        int gap = 3;
        int widgetsX = 107;
        int widgetsY = 225;

        textFieldTeamName = new EditBox(font, guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 0, 110, 20, new TextComponent(teamName));
        textFieldTeamName.setTextColor(-1);
        textFieldTeamName.setTextColorUneditable(-1);
        textFieldTeamName.setBordered(true);
        textFieldTeamName.setMaxLength(24);
        textFieldTeamName.setValue(teamName);

        //textFieldTeamName.setResponder();

        addRenderableWidget(textFieldTeamName);
        setInitialFocus(textFieldTeamName);

        addRenderableOnly(new BlackShowingTextField(guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 4, 110, 20, 45, 0, getMaxRecruitsPerPlayerString()));

        if (leaderInfo != null) {
            this.leaderWidget = new SelectedPlayerWidget(font, guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 1, 110, 20, new TextComponent("x"), // Button label
                    () -> {
                        leaderInfo = null;
                        this.leaderWidget.setPlayer(null, null);
                        this.setWidgets();
                    }
            );

            this.leaderWidget.setPlayer(leaderInfo.getUUID(), leaderInfo.getName());
            addRenderableWidget(this.leaderWidget);
        } else {
            Button selectPlayerButton = addRenderableWidget(new Button(guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 1, 110, 20, SelectPlayerScreen.TITLE,
                    button -> {
                        minecraft.setScreen(new SelectPlayerScreen(this, player, SelectPlayerScreen.TITLE, SelectPlayerScreen.BUTTON_SELECT, SelectPlayerScreen.BUTTON_SELECT_TOOLTIP, false, PlayersList.FilterType.SAME_TEAM,
                                (playerInfo) -> {
                                    this.leaderInfo = playerInfo;
                                    minecraft.setScreen(this);
                                }
                        ));
                    },
                    (button1, poseStack, i, i1) -> {
                        this.renderTooltip(poseStack, SELECT_LEADER_TOOLTIP, i, i1);
                    }
            ));
        }

        teamColorDropdownMatrix = new ColorChatFormattingSelectionDropdownMatrix(this, guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 2, 110, 20,
                teamColors,
                this::setTeamColor
        );
        addRenderableWidget(teamColorDropdownMatrix);


        unitColorDropdownMatrix = new ColorSelectionDropdownMatrix(this, guiLeft + widgetsX, guiTop + ySize - widgetsY + (20 + gap ) * 3, 110, 20,
                unitColors,
                this::setUnitColor
        );
        addRenderableWidget(unitColorDropdownMatrix);

        addRenderableWidget(new Button(guiLeft + widgetsX + 20, guiTop + ySize - widgetsY + (20 + gap ) * 4, 20, 20,new TextComponent("+"),
        (button)-> {
            if(hasShiftDown()){
                maxRecruitsPerPlayer += 5;
            }
            else
                maxRecruitsPerPlayer++;

            if(maxRecruitsPerPlayer > maxRecruitsPerPlayerConfigSetting) maxRecruitsPerPlayer = maxRecruitsPerPlayerConfigSetting;

            setWidgets();
        }));

        addRenderableWidget(new Button(guiLeft + widgetsX + 75, guiTop + ySize - widgetsY + (20 + gap ) * 4, 20, 20,new TextComponent("-"),
        (button)-> {
            if(hasShiftDown()){
                maxRecruitsPerPlayer -= 5;
            }
            else
                maxRecruitsPerPlayer--;

            if(maxRecruitsPerPlayer < 0) maxRecruitsPerPlayer = 0;

            setWidgets();
        }));
        //this.banner = container.getBanner();
        if(recruitsTeam == null){
            saveButton = new Button(guiLeft + 30, guiTop + ySize - 100, 175, 20, CREATE,
                btn -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCreateTeam(this.getCorrectFormat(textFieldTeamName.getValue().strip()), banner, teamColor, unitColors.indexOf(unitColor)));
                    minecraft.setScreen(new TeamInspectionScreen(new TeamMainScreen(player), player));
                }
            );

            saveButton.active = checkCreationCondition();
            addRenderableWidget(saveButton);
        }
        else {
            addRenderableWidget(new Button(guiLeft + 117, guiTop + ySize - 102, 75, 20, BACK,
                    btn -> {
                        minecraft.setScreen(parent);
                    }
            ));

            saveButton = new Button(guiLeft + 30, guiTop + ySize - 102, 75, 20, SAVE,
                    btn -> {
                        recruitsTeam.setTeamLeaderID(this.leaderInfo.getUUID());
                        recruitsTeam.setTeamName(textFieldTeamName.getValue());
                        recruitsTeam.setTeamColor(teamColor.getId());
                        if(banner != null){
                            recruitsTeam.setBanner(banner.getTag());
                        }
                        recruitsTeam.setUnitColor((byte) unitColors.indexOf(unitColor));
                        recruitsTeam.setMaxNPCsPerPlayer(maxRecruitsPerPlayer);

                        Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveTeamSettings(recruitsTeam, recruitsTeam.getTeamName()));
                    }
            );

            saveButton.active = checkEditCondition();
            addRenderableWidget(saveButton);
        }

        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + ySize - widgetsY + (20 + gap ) * 0, 60, 20, TEAM_NAME));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + ySize - widgetsY + (20 + gap ) * 1, 60, 20, LEADER));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + ySize - widgetsY + (20 + gap ) * 2, 60, 20, TEAM_COLOR));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + ySize - widgetsY + (20 + gap ) * 3, 60, 20, UNITS_COLOR));
        addRenderableOnly(new BlackShowingTextField(guiLeft + textsX, guiTop + ySize - widgetsY + (20 + gap ) * 4, 60, 20, MAX_RECRUITS));
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

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }
    int x1 = 15;
    int y1 = 61;
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        super.render(poseStack, mouseX, mouseY, delta);
        if(bannerRenderer != null) bannerRenderer.renderBanner(poseStack, this.guiLeft + x1, guiTop + y1, this.width, this.height, 50);
    }

    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        int crownX = width / 2 - 23;
        int crownY = guiTop + 41;

        font.draw(poseStack, getTitle(), guiLeft + xSize / 2 - font.width(getTitle()) / 2, guiTop + 5, FONT_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, LEADER_CROWN);
        GuiComponent.blit(poseStack, crownX, crownY, 0, 0, 16, 16, 16, 16);
    }

    public Component getMaxRecruitsPerPlayerString() {
        return new TranslatableComponent("gui.recruits.two_values_with_slash", maxRecruitsPerPlayer, maxRecruitsPerPlayerConfigSetting);
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
        boolean nameLength = this.textFieldTeamName.getValue().length() >= 3 && this.textFieldTeamName.getValue().length() <= 24;
        //boolean sufficentEmeralds = player. >= creationPrice && !player.isCreative();

        return !this.banner.isEmpty() && nameLength && this.leaderInfo != null;
    }

    private boolean checkEditCondition(){
        boolean nameLength = this.textFieldTeamName.getValue().length() >= 3 && this.textFieldTeamName.getValue().length() <= 24;
        //boolean sufficentEmeralds = player. >= creationPrice && !player.isCreative();

        return nameLength && this.leaderInfo != null;
    }

    @Override
    public Component getTitle() {
        return title;
    }
}
