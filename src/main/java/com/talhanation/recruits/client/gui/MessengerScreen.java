package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.inventory.MessengerContainer;
import com.talhanation.recruits.network.MessageSendMessenger;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;


public class MessengerScreen extends ScreenBase<MessengerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/blank_gui.png");
    private final Player player;
    private final MessengerEntity recruit;
    private EditBox textFieldPlayer;
    private MultiLineEditBox textFieldMessage;
    private int leftPos;
    private int topPos;

    public static String message = "Message";

    private static final MutableComponent TOOLTIP_MESSENGER = Component.translatable("gui.recruits.inv.tooltip.messenger");
    private static final MutableComponent BUTTON_MESSENGER = Component.translatable("gui.recruits.inv.text.send_messenger");
    private static final int fontColor = 4210752;

    public MessengerScreen(MessengerContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.imageWidth = 197;
        this.imageHeight = 250;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if(textFieldPlayer.isFocused()){
            setFocused(textFieldPlayer);
            return textFieldPlayer.keyPressed(key, a, b) || textFieldPlayer.canConsumeInput() || super.keyPressed(key, a, b);
        }
        else{
            setFocused(textFieldMessage);
            return textFieldMessage.keyPressed(key, a, b) || textFieldMessage.isFocused() || super.keyPressed(key, a, b);
        }

    }
    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        Component componentPlayer = Component.literal("Player");
        String targetPlayerName = recruit.getTargetPlayerName();
        if (targetPlayerName != null && !targetPlayerName.isEmpty() && !targetPlayerName.isBlank())
            componentPlayer = Component.literal(targetPlayerName);

        this.textFieldPlayer = new EditBox(font, leftPos + 3, topPos + 17, 186, 14, componentPlayer);
        this.textFieldPlayer.setMaxLength(23);
        this.textFieldPlayer.setBordered(true);
        this.textFieldPlayer.setVisible(true);
        this.textFieldPlayer.setTextColor(-1);
        this.textFieldPlayer.setValue(componentPlayer.getString());

        addRenderableWidget(textFieldPlayer);

        this.textFieldMessage = new MultiLineEditBox(font, leftPos + 3, topPos + 47, 186, 150, Component.empty(), Component.empty());
        this.textFieldMessage.setValue(message);

        addRenderableWidget(textFieldMessage);

        this.setInitialFocus(this.textFieldPlayer);
        this.textFieldPlayer.setFocused(true);

        setSendButton();
    }
    protected void containerTick() {
        super.containerTick();
        textFieldPlayer.tick();
        textFieldMessage.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textFieldPlayer.isFocused()) {
            this.textFieldPlayer.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        if (this.textFieldMessage.isFocused()) {
            this.textFieldMessage.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }

    private void setSendButton() {
        Button sendButton = addRenderableWidget(new ExtendedButton(leftPos + 33, topPos + 200, 128, 20, BUTTON_MESSENGER,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSendMessenger(recruit.getUUID(), textFieldPlayer.getValue(), textFieldMessage.getValue(), true));
                    this.onClose();
                }
        ));
        sendButton.setTooltip(Tooltip.create(TOOLTIP_MESSENGER));
    }

    @Override
    public void onClose() {
        super.onClose();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageSendMessenger(recruit.getUUID(), textFieldPlayer.getValue(), textFieldMessage.getValue(), false));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        //Info
        int fontColor = 4210752;

        guiGraphics.drawString(font, "Player:", 5, 5, fontColor, false);
        guiGraphics.drawString(font, "Message:", 5, 35, fontColor, false);

        if(!recruit.getMainHandItem().isEmpty()){
            guiGraphics.renderFakeItem(recruit.getMainHandItem(), 140, 202);
            guiGraphics.renderItemDecorations(font, recruit.getMainHandItem(),140, 202);
        }
    }
}