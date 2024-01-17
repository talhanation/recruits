package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.inventory.MessengerContainer;
import com.talhanation.recruits.network.MessageSendMessenger;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MessengerScreen extends ScreenBase<MessengerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/messenger_gui.png");
    private final Player player;
    private final MessengerEntity recruit;
    private EditBox textFieldPlayer;
    private EditBox textFieldMessage;
    private int leftPos;
    private int topPos;

    private static final MutableComponent TOOLTIP_MESSENGER = Component.translatable("gui.recruits.inv.tooltip.messenger");
    private static final MutableComponent BUTTON_MESSENGER = Component.translatable("gui.recruits.inv.text.send_messenger");
    private static final int fontColor = 4210752;

    //private boolean keepTeam;

    public MessengerScreen(MessengerContainer container, Inventory playerInventory, Component title) {
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

        setSendButton();

        Component componentPlayer = Component.literal("Player");
        String targetPlayerName = recruit.getTargetPlayerName();
        if (targetPlayerName != null && !targetPlayerName.isEmpty() && !targetPlayerName.isBlank())
            componentPlayer = Component.literal(targetPlayerName);

        this.textFieldPlayer = new EditBox(font, leftPos + 16, topPos + 19, 160, 20, componentPlayer);
        this.textFieldPlayer.setValue(componentPlayer.getString());
        this.textFieldPlayer.setBordered(true);
        this.textFieldPlayer.setVisible(true);
        this.textFieldPlayer.setTextColor(-1);
        this.textFieldPlayer.setMaxLength(23);

        addRenderableWidget(textFieldPlayer);

        Component componentMessage = Component.literal("Message");
        String message = recruit.getMessage();
        if (message != null && !message.isBlank() && !message.isEmpty()) componentMessage = Component.literal(message);

        textFieldMessage = new EditBox(font, leftPos + 16, topPos + 54, 160, 20, componentMessage);
        this.textFieldMessage.setValue(componentMessage.getString());
        this.textFieldMessage.setBordered(true);
        this.textFieldMessage.setVisible(true);
        this.textFieldMessage.setTextColor(-1);
        this.textFieldMessage.setMaxLength(1453);

        addRenderableWidget(textFieldMessage);

        this.setInitialFocus(this.textFieldPlayer);
        this.textFieldPlayer.setFocused(true);

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
        Button sendButton = addRenderableWidget(new ExtendedButton(leftPos + 33, topPos + 101, 128, 20, BUTTON_MESSENGER,
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
        guiGraphics.drawString(font, "Player:", 16, 9, fontColor);

        guiGraphics.drawString(font, "Message:", 16, 42, fontColor);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 16, 127, fontColor);
    }
}