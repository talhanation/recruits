package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.entities.MessengerEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MessengerMainScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");

    private static final Component BUTTON_MESSAGES = Component.translatable("gui.recruits.messenger.tab.messages");
    private static final Component BUTTON_TREATIES = Component.translatable("gui.recruits.messenger.tab.treaties");
    private static final Component TITLE = Component.translatable("gui.recruits.messenger.main_title");

    private final Player player;
    private final MessengerEntity messenger;

    public MessengerMainScreen(MessengerEntity messenger, Player player) {
        super(TITLE, 195,160);
        this.messenger = messenger;
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

        Button messagesButton = new ExtendedButton(btnX, guiTop + 30, btnWidth, 20, BUTTON_MESSAGES,
                button -> minecraft.setScreen(new MessengerScreen(messenger, player))
        );
        addRenderableWidget(messagesButton);


        Button treatyButton = new ExtendedButton(btnX, guiTop + 55, btnWidth, 20, BUTTON_TREATIES,
                button -> minecraft.setScreen(new MessengerTreatyScreen(messenger, player))
        );
        treatyButton.active = ClientManager.ownFaction != null && ClientManager.ownFaction.getTeamLeaderUUID().equals(player.getUUID());
        addRenderableWidget(treatyButton);
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
