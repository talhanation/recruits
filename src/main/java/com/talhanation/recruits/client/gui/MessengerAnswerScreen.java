package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.MessageAnswerMessenger;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class MessengerAnswerScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/blank_gui.png");
    private final Player player;
    private final MessengerEntity messenger;
    private RecruitsMultiLineEditBox textFieldMessage;

    private final String message;

    private final RecruitsPlayerInfo playerInfo;
    private static final MutableComponent BUTTON_OK = Component.translatable("gui.recruits.inv.text.ok_messenger");
    public MessengerAnswerScreen(MessengerEntity messenger, Player player, String message, RecruitsPlayerInfo playerInfo) {
        super(Component.literal(""), 197,250);
        this.player = player;
        this.messenger = messenger;
        this.message = message;
        this.playerInfo = playerInfo;
    }

    @Override
    protected void init() {
        super.init();
        this.textFieldMessage = new RecruitsMultiLineEditBox(font, guiLeft + 3, guiTop + ySize - 215, 186, 165, Component.empty(), Component.empty());
        this.textFieldMessage.setValue(message);
        this.textFieldMessage.setEnableEditing(false);
        this.textFieldMessage.changeFocus(false);


        addRenderableWidget(textFieldMessage);

        setOKButton();
    }
    public void tick() {
        super.tick();
        if(textFieldMessage != null) this.textFieldMessage.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }

    private void setOKButton() {
        Button sendButton = addRenderableWidget(new Button(guiLeft + 33, guiTop + ySize - 50, 128, 20, BUTTON_OK,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnswerMessenger(messenger.getUUID()));

                    onClose();
                }
        ));
    }

    @Override
    public void onClose() {
        super.onClose();

    }

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float delta) {
        String targetPlayer = playerInfo.getName();
        String owner = this.messenger.getOwnerName();
        String unit = "min";
        int rawtime = this.messenger.getWaitingTime();
        int time = rawtime / 20;
        if (time <= 100) unit = "sec";
        else time = time / 60;

        //Info
        int fontColor = 4210752;
        font.draw(matrixStack, "From:", guiLeft + 9, guiTop + 9, fontColor);
        font.draw(matrixStack, "To:", guiLeft + 9,  guiTop + 20, fontColor);
        font.draw(matrixStack, "" + owner, guiLeft + 50,  guiTop + 9, fontColor);
        font.draw(matrixStack, "" + targetPlayer, guiLeft + 50,  guiTop + 20, fontColor);

        font.draw(matrixStack, "Time: " + time + unit, guiLeft + 130, guiTop + 9, fontColor);

        if(!messenger.getMainHandItem().isEmpty()){
            itemRenderer.renderGuiItem(messenger.getMainHandItem(), guiLeft + 120, guiTop + ySize - 48);
            itemRenderer.renderGuiItemDecorations(font, messenger.getMainHandItem(),guiLeft + 120, guiTop + ySize - 48);
        }
    }
}