package com.talhanation.recruits.client.gui.diplomacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.faction.SelectFactionScreen;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.network.MessageAddEmbargo;
import com.talhanation.recruits.network.MessageAddEmbargoFaction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class EmbargoAddScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");

    private static final Component TITLE        = Component.translatable("gui.recruits.embargo.add_title");
    private static final Component BUTTON_PLAYER  = Component.translatable("gui.recruits.embargo.button_player");
    private static final Component BUTTON_FACTION = Component.translatable("gui.recruits.embargo.button_faction");

    private static final Component SELECT_PLAYER_TITLE   = Component.translatable("gui.recruits.select_player_screen.title");
    private static final Component SELECT_PLAYER_BUTTON  = Component.translatable("gui.recruits.embargo.button_add");
    private static final Component SELECT_PLAYER_TOOLTIP = Component.translatable("gui.recruits.embargo.button_add_tooltip");

    private static final Component SELECT_FACTION_TITLE   = Component.translatable("gui.recruits.select_faction_screen.title");
    private static final Component SELECT_FACTION_BUTTON  = Component.translatable("gui.recruits.embargo.button_add");
    private static final Component SELECT_FACTION_TOOLTIP = Component.translatable("gui.recruits.embargo.button_add_tooltip");

    private final EmbargoScreen embargoScreen;
    private final Player player;

    public EmbargoAddScreen(EmbargoScreen embargoScreen, Player player) {
        super(TITLE, 195, 160);
        this.embargoScreen = embargoScreen;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        setButtons();
    }

    private void setButtons() {
        clearWidgets();

        int btnWidth = 128;
        int btnX = guiLeft + (xSize - btnWidth) / 2;

        // [Player] → opens SelectPlayerScreen filtered to players with a faction
        Button playerButton = new ExtendedButton(btnX, guiTop + 30, btnWidth, 20, BUTTON_PLAYER,
                button -> minecraft.setScreen(new SelectPlayerScreen(
                        this, player,
                        SELECT_PLAYER_TITLE, SELECT_PLAYER_BUTTON, SELECT_PLAYER_TOOLTIP,
                        false, PlayersList.FilterType.NONE,
                        (playerInfo) -> {
                            if (playerInfo != null) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAddEmbargo(playerInfo.getUUID()));
                            }
                            minecraft.setScreen(embargoScreen);
                        }
                ))
        );
        addRenderableWidget(playerButton);

        // [Faction] → opens SelectFactionScreen
        Button factionButton = new ExtendedButton(btnX, guiTop + 55, btnWidth, 20, BUTTON_FACTION,
                button -> minecraft.setScreen(new SelectFactionScreen(
                        this, player,
                        SELECT_FACTION_TITLE, SELECT_FACTION_BUTTON, SELECT_FACTION_TOOLTIP,
                        (faction) -> {
                            if (faction != null) {
                                Main.SIMPLE_CHANNEL.sendToServer(
                                        new MessageAddEmbargoFaction(faction.getStringID())
                                );
                            }
                            minecraft.setScreen(embargoScreen);
                        },
                        false
                ))
        );
        // Cannot embargo own faction
        factionButton.active = ClientManager.ownFaction != null;
        addRenderableWidget(factionButton);
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
        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR, false);
    }
}
