package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.MessageSendMessenger;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;


public class MessengerScreen extends RecruitsScreenBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/blank_gui.png");
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private final Player player;
    public static RecruitsPlayerInfo playerInfo;
    private final MessengerEntity messenger;
    private MultiLineEditBox textFieldMessage;

    public static String message = "Message";
    private SelectedPlayerWidget selectedPlayerWidget;
    private static final MutableComponent TOOLTIP_MESSENGER = Component.translatable("gui.recruits.inv.tooltip.messenger");
    private static final MutableComponent BUTTON_SEND_MESSENGER = Component.translatable("gui.recruits.inv.text.send_messenger");
    private static final int fontColor = 4210752;
    public MessengerScreen(MessengerEntity messenger, Player player) {
        super( Component.literal(""), 197,250);
        this.player = player;
        this.messenger = messenger;
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        setFocused(textFieldMessage);
        return textFieldMessage.keyPressed(key, a, b) || textFieldMessage.isFocused() || super.keyPressed(key, a, b);
    }
    @Override
    protected void init() {
        super.init();

        setButtons();
    }
    public void tick() {
        super.tick();
        if(textFieldMessage != null) textFieldMessage.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textFieldMessage.isFocused()) {
            this.textFieldMessage.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }

    private void setButtons() {
        clearWidgets();

        this.textFieldMessage = new MultiLineEditBox(font, guiLeft + 3, guiTop + ySize - 203,  186, 150, Component.literal(""), Component.literal(""));
        this.textFieldMessage.setValue(message);
        addRenderableWidget(textFieldMessage);

        Button sendButton = addRenderableWidget(new Button(guiLeft + 33, guiTop + ySize - 52 , 128, 20, BUTTON_SEND_MESSENGER,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSendMessenger(messenger.getUUID(), playerInfo, textFieldMessage.getValue(), true));
                    this.onClose();
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_MESSENGER, i, i1);
                }
        ));
        sendButton.active = playerInfo != null;

        if(playerInfo != null){
            this.selectedPlayerWidget = new SelectedPlayerWidget(font, guiLeft + 33, guiTop + ySize - 235, 128, 20, Component.literal("x"), // Button label
                    () -> {
                        playerInfo = null;
                        this.selectedPlayerWidget.setPlayer(null, null);
                        this.setButtons();
                    }
            );

            this.selectedPlayerWidget.setPlayer(playerInfo.getUUID(), playerInfo.getName());
            addRenderableWidget(this.selectedPlayerWidget);
        }
        else
        {
            Button selectPlayerButton = addRenderableWidget(new Button(guiLeft + 33, guiTop + ySize - 235, 128, 20, SelectPlayerScreen.TITLE,
                    button -> {
                        minecraft.setScreen(new SelectPlayerScreen(this, player, SelectPlayerScreen.TITLE, SelectPlayerScreen.BUTTON_SELECT, SelectPlayerScreen.BUTTON_SELECT_TOOLTIP, false, PlayersList.FilterType.NONE,
                                (playerInfo) -> {
                                    MessengerScreen.playerInfo = playerInfo;
                                    minecraft.setScreen(this);
                                }
                        ));

                    },
                    (button1, poseStack, i, i1) -> {
                        this.renderTooltip(poseStack, TOOLTIP_MESSENGER, i, i1);
                    }
            ));
        }
    }

    public void onClose(){
        super.onClose();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageSendMessenger(messenger.getUUID(), playerInfo, textFieldMessage.getValue(), false));
    }

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }
    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        int fontColor = 4210752;

        font.draw(poseStack, "Player:", guiLeft + 5, guiTop + 5, fontColor);
        font.draw(poseStack, "Message:", guiLeft + 5, guiTop + 35, fontColor);

        if(!messenger.getMainHandItem().isEmpty()){
            itemRenderer.renderGuiItem(messenger.getMainHandItem(), guiLeft + 140, guiTop + ySize - 48);
            itemRenderer.renderGuiItemDecorations(font, messenger.getMainHandItem(),guiLeft + 140, guiTop + ySize - 48);
        }
    }
}