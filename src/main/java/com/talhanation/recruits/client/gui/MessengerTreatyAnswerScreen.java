package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.SelectedFactionWidget;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.MessageAnswerTreaty;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

public class MessengerTreatyAnswerScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");

    private static final Component TITLE        = Component.translatable("gui.recruits.messenger.treaty_title");
    private static final Component BUTTON_ACCEPT  = Component.translatable("gui.recruits.messenger.accept_treaty");
    private static final Component BUTTON_DECLINE  = Component.translatable("gui.recruits.messenger.decline_treaty");
    private static final Component TEXT_FACTION = Component.translatable("gui.recruits.messenger.text_faction");
    private static final Component TEXT_DURATION = Component.translatable("gui.recruits.messenger.text_duration");
    private static final Component DESCRIPTION_TEXT = Component.translatable("description.recruits.peaceTreaty");

    private final Player player;
    private final MessengerEntity messenger;
    private final int durationHours;
    private final RecruitsPlayerInfo senderInfo;

    private SelectedFactionWidget senderFactionWidget;
    private RecruitsMultiLineEditBox descriptionBox;

    public MessengerTreatyAnswerScreen(MessengerEntity messenger, Player player, int durationHours, RecruitsPlayerInfo senderInfo) {
        super(TITLE, 195, 200);
        this.messenger = messenger;
        this.player = player;
        this.durationHours = durationHours;
        this.senderInfo = senderInfo;
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
        setWidgets();
    }

    private void setWidgets() {
        clearWidgets();

        int widgetX = guiLeft + 10;
        int widgetY = guiTop + 30;
        int widgetW = 175;
        int widgetH = 30;

        senderFactionWidget = new SelectedFactionWidget(
                font, widgetX, widgetY, widgetW, widgetH,
                Component.literal(""),
                () -> {}
        );
        if (senderInfo != null && senderInfo.getFaction() != null) {
            senderFactionWidget.setFaction(senderInfo.getFaction());
        }
        senderFactionWidget.setButtonVisible(false);
        addRenderableWidget(senderFactionWidget);

        int durationY = guiTop + 65;
        String text = TEXT_DURATION.getString() + " " + durationHours+"h";
        addRenderableOnly(new BlackShowingTextField(widgetX, durationY, widgetW, 20, 3, 0, Component.literal(text)));

        int descY = durationY + 23;
        int descH = 45;
        descriptionBox = new RecruitsMultiLineEditBox(font, guiLeft + 10, descY, 175, descH, Component.empty(), Component.empty());
        descriptionBox.setValue(DESCRIPTION_TEXT.getString());
        descriptionBox.setEnableEditing(false);
        descriptionBox.setScrollAmount(0);
        addRenderableWidget(descriptionBox);

        int buttonY = guiTop + 135;
        addRenderableWidget(new ExtendedButton(guiLeft + 10, buttonY, 80, 20, BUTTON_ACCEPT,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnswerTreaty(messenger.getUUID(), true));
                    onClose();
                }
        ));

        addRenderableWidget(new ExtendedButton(guiLeft + 105, buttonY, 80, 20, BUTTON_DECLINE,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnswerTreaty(messenger.getUUID(), false));
                    onClose();
                }
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (descriptionBox != null) descriptionBox.tick();
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
        // Title – centred
        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 8, FONT_COLOR, false);
    }
}
