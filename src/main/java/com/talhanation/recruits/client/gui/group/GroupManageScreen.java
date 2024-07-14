package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.GroupManageContainer;
import com.talhanation.recruits.network.MessageApplyNoGroup;
import com.talhanation.recruits.network.MessageServerSavePlayerGroups;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;
@OnlyIn(Dist.CLIENT)
public class GroupManageScreen extends ScreenBase<GroupManageContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/group_list_gui.png");
    private Player player;
    private int leftPos;
    private int topPos;
    private GroupListWidget groupListWidget;
    public static List<RecruitsGroup> groups;
    public boolean setList;
    private GroupListWidget.GroupEntry selectedEntry;

    public GroupManageScreen(GroupManageContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 197;
        imageHeight = 250;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        setList = false;

        addRenderableWidget(createAddGroupButton(leftPos + 14, topPos + 215));

        addRenderableWidget(createEditGroupButton(leftPos + 74, topPos + 215));

        addRenderableWidget(createRemoveGroupButton(leftPos + 134, topPos + 215));
    }

    @Override
    public void onClose() {
        super.onClose();
        saveGroups();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if(groups != null && !groups.isEmpty() && !setList){
            this.setList();
            setList = true;
        }
    }

    private void setList(){
        this.groupListWidget = new GroupListWidget(this, 166, topPos + 22, topPos + 22 + 182, groups);
        this.groupListWidget.setLeftPos(leftPos + 16);

        this.groupListWidget.setRenderBackground(false);
        this.groupListWidget.setRenderTopAndBottom(false);
        this.groupListWidget.setSelected(groupListWidget.getFirstElement());
        addRenderableWidget(groupListWidget);
    }

    public Font getFont() {
        return this.font;
    }
    protected void render(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(    1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        this.groupListWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

    }
    public void setSelected(GroupListWidget.GroupEntry groupEntry) {
        this.selectedEntry = groupEntry;
    }

    private ExtendedButton createRemoveGroupButton(int x, int y) {
        return new ExtendedButton(x, y, 50, 18, Component.literal("Remove"), button -> {
            if (selectedEntry != null) {

                RecruitsGroup group = selectedEntry.getGroup();
                Main.SIMPLE_CHANNEL.sendToServer(new MessageApplyNoGroup(player.getUUID(), group.getId()));

                groups.remove(group);
                this.saveGroups();
                this.groupListWidget.removeGroup(selectedEntry);

                this.groupListWidget.refreshList();
                this.groupListWidget.setScrollAmount(0);
                this.init();
            }
        });
    }

    private ExtendedButton createAddGroupButton(int x, int y) {
        return new ExtendedButton(x, y, 50, 18, Component.literal("Add"), btn -> {
            this.minecraft.setScreen(new EditOrAddGroupScreen(this));
        });
    }

    private ExtendedButton createEditGroupButton(int x, int y) {
        return new ExtendedButton(x, y, 50, 18, Component.literal("Edit"), btn -> {
            if(selectedEntry != null) this.minecraft.setScreen(new EditOrAddGroupScreen(this, selectedEntry.getGroup()));
        });
    }

    public void saveGroups() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups, false));
    }
}