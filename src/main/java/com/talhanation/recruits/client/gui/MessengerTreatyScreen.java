package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.faction.SelectFactionScreen;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.SelectedFactionWidget;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.MessageSendTreaty;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

public class MessengerTreatyScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final int FONT_COLOR_FIELD = FastColor.ARGB32.color(255, 255, 255, 255);

    private static final Component TITLE = Component.translatable("gui.recruits.messenger.treaty_title");
    private static final Component BUTTON_SEND = Component.translatable("gui.recruits.messenger.send_treaty");
    private static final Component BUTTON_SELECT_FACTION = Component.translatable("gui.recruits.messenger.select_faction");
    private static final Component LABEL_FACTION = Component.translatable("gui.recruits.messenger.text_faction");
    private static final Component LABEL_DURATION = Component.translatable("gui.recruits.messenger.text_duration");

    private final Player player;
    private final MessengerEntity messenger;

    public static RecruitsFaction selectedFaction;
    private int durationHours = 1;

    private Button sendButton;
    private Button plusButton;
    private Button minusButton;
    private SelectedFactionWidget selectedFactionWidget;

    public MessengerTreatyScreen(MessengerEntity messenger, Player player) {
        super(TITLE, 195,160);
        this.messenger = messenger;
        this.player = player;
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, a, b);
    }

    @Override
    protected void init() {
        super.init();
        if (durationHours < 1) durationHours = 1;
        if (durationHours > 48) durationHours = 48;
        setButtons();
    }

    private void setButtons() {
        clearWidgets();

        int widgetX = guiLeft + 23;
        int widgetY = guiTop + 30;
        int widgetW = 148;
        int widgetH = 30;

        if (selectedFaction != null) {
            selectedFactionWidget = new SelectedFactionWidget(font, widgetX, widgetY, widgetW, widgetH, Component.literal("x"),
                () -> {
                    selectedFaction = null;
                    setButtons();
                }
            );
            selectedFactionWidget.setFaction(selectedFaction);
            addRenderableWidget(selectedFactionWidget);
        } else {
            addRenderableWidget(new ExtendedButton(widgetX, widgetY + 5, widgetW, 20, BUTTON_SELECT_FACTION,
                    button -> openFactionSelect()
            ));
        }
        int durationY = guiTop + 90;

        addRenderableOnly(new BlackShowingTextField( guiLeft + 23, durationY, widgetW, 20, widgetW/2 - 10, 0, Component.literal("" + durationHours + "h")));

        minusButton = addRenderableWidget(new ExtendedButton(guiLeft + 23, durationY, 20, 20,
                Component.literal("-"),
                button -> {
                    int x = 1;
                    if(hasShiftDown()) x = 5;

                    durationHours = Math.max(1, durationHours - x);
                    setButtons();
                }
        ));
        minusButton.active = durationHours > 1;


        plusButton = addRenderableWidget(new ExtendedButton(guiLeft + 151, durationY, 20, 20,
                Component.literal("+"),
                button -> {
                    int x = 1;
                    if(hasShiftDown()) x = 5;

                    durationHours = Math.min(48, durationHours + x);
                    setButtons();
                }
        ));
        plusButton.active = durationHours < 48;

        // Send button
        sendButton = addRenderableWidget(new ExtendedButton(guiLeft + 23, guiTop + ySize - 30, 148, 20,
                BUTTON_SEND,
                button -> {
                    if (selectedFaction != null && selectedFaction.getTeamLeaderUUID() != null) {
                        RecruitsPlayerInfo leaderInfo = new RecruitsPlayerInfo(
                                selectedFaction.getTeamLeaderUUID(),
                                selectedFaction.getTeamLeaderName(),
                                selectedFaction
                        );
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageSendTreaty(messenger.getUUID(), leaderInfo, durationHours, true));
                        this.onClose();
                    }
                }
        ));
        sendButton.active = selectedFaction != null;
    }

    private void openFactionSelect() {
        minecraft.setScreen(new SelectFactionScreen(this, player,
                Component.literal("Select Faction for Treaty"),
                Component.literal("Select"),
                Component.literal("Select this Faction for Treaty"),
                faction -> {
                    MessengerTreatyScreen.selectedFaction = faction;
                    minecraft.setScreen(this);
                },
                false
        ));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 8, FONT_COLOR, false);

        guiGraphics.drawString(font, LABEL_FACTION, guiLeft + 5, guiTop + 18, FONT_COLOR, false);
        guiGraphics.drawString(font, LABEL_DURATION, guiLeft + 5, guiTop + 77, FONT_COLOR, false);
    }
}

