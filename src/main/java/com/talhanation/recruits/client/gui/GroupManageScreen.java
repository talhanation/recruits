package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.RecruitsGroup;
import com.talhanation.recruits.inventory.GroupManageContainer;
import com.talhanation.recruits.network.MessageRemoveGroupApplyNoGroup;
import com.talhanation.recruits.network.MessageServerSavePlayerGroups;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class GroupManageScreen extends ScreenBase<GroupManageContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_main_gui.png");
    public static List<RecruitsGroup> groups;
    Player player;
    private int leftPos;
    private int topPos;
    private static final MutableComponent ADD_GROUP = Component.translatable("gui.recruits.group_creation.add");
    private static final MutableComponent CANCEL = Component.translatable("gui.recruits.group_creation.cancel");
    private EditBox textField;
    private boolean buttonsSet;
    public GroupManageScreen(GroupManageContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 250;
        imageHeight = 83;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

    }

    @Override
    public void onClose() {
        super.onClose();
        groups = null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(groups != null && !groups.isEmpty() && !buttonsSet){
            setAddWidgets();
            this.buttonsSet = true;
        }

        if(textField != null && textField.isFocused()){
            textField.tick();
        }
    }

    private void setAddWidgets() {
        textField = new EditBox(font, leftPos + 20, topPos + 10, 200, 15, Component.literal(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);
        textField.setFocused(true);
        textField.setCanLoseFocus(false);
        addRenderableWidget(textField);
        setInitialFocus(textField);


        addRenderableWidget(new ExtendedButton(leftPos + 20, topPos + 39, 100, 20, ADD_GROUP, btn -> {
            groups.add(new RecruitsGroup(groups.size(), textField.getValue(), false));

            Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups));
            onClose();
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 130, topPos + 39, 100, 20, CANCEL, btn -> {
            onClose();
        }));
    }

    protected void render(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    private ExtendedButton createRemoveGroupButton(RecruitsGroup group, int x, int y) {
        return new ExtendedButton(x + 18,y + 38,12,12,Component.literal("-"), button -> {
            if(group != null){
                Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveGroupApplyNoGroup(player.getUUID(), group.getId()));
                buttonsSet = false;
                //this.setButtons();
                //removeGroupButton = null;
                //groups.remove(group);
                //SEND TO SERVER
            }
        });
    }

}
