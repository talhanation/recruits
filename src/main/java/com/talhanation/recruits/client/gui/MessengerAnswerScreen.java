package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.inventory.MessengerAnswerContainer;
import com.talhanation.recruits.network.MessageAnswerMessenger;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MessengerAnswerScreen extends ScreenBase<MessengerAnswerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/blank_gui.png");
    private final Player player;
    private final MessengerEntity recruit;
    private RecruitsMultiLineEditBox textFieldMessage;
    private int leftPos;
    private int topPos;

    public static String message = "";
    private static final MutableComponent BUTTON_OK = Component.translatable("gui.recruits.inv.text.ok_messenger");
    private static final int fontColor = 4210752;

    public MessengerAnswerScreen(MessengerAnswerContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.imageWidth = 197;
        this.imageHeight = 250;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.textFieldMessage = new RecruitsMultiLineEditBox(font, leftPos + 3, topPos + 35, 186, 165, Component.empty(), Component.empty());
        this.textFieldMessage.setValue(message);
        this.textFieldMessage.setEnableEditing(false);

        addRenderableWidget(textFieldMessage);

        setOKButton();
    }
    protected void containerTick() {
        super.containerTick();
        this.textFieldMessage.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }

    private void setOKButton() {
        Button sendButton = addRenderableWidget(new ExtendedButton(leftPos + 33, topPos + 200, 128, 20, BUTTON_OK,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnswerMessenger(recruit.getUUID()));
                    this.onClose();
                }
        ));
    }

    @Override
    public void onClose() {
        super.onClose();

    }

    protected void render(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        guiGraphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        String targetPlayer = this.recruit.getTargetPlayerName();
        String owner = this.recruit.getOwnerName();
        String unit = "min";
        int rawtime = this.recruit.getWaitingTime();
        int time = rawtime / 20;
        if (time <= 100) unit = "sec";
        else time = time / 60;

        //Info
        int fontColor = 4210752;
        guiGraphics.drawString(font, "From:", 9, 9, fontColor, false);
        guiGraphics.drawString(font, "To:", 9, 20, fontColor, false);
        guiGraphics.drawString(font, "" + owner, 50, 9, fontColor, false);
        guiGraphics.drawString(font, "" + targetPlayer, 50, 20, fontColor, false);

        guiGraphics.drawString(font, "Time: " + time + unit, 130, 9, fontColor, false);

        if(!recruit.getMainHandItem().isEmpty()){
            guiGraphics.renderFakeItem(recruit.getMainHandItem(), 120, 202);
            guiGraphics.renderItemDecorations(font, recruit.getMainHandItem(),120, 202);
        }
    }
}