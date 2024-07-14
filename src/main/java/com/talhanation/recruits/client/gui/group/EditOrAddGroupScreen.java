package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class EditOrAddGroupScreen extends Screen {
    private EditBox groupNameField;
    private final GroupManageScreen parent;
    private RecruitsGroup groupToEdit;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_main_gui.png");
    public EditOrAddGroupScreen(GroupManageScreen parent) {
        this(parent, null);
    }

    public EditOrAddGroupScreen(GroupManageScreen parent, RecruitsGroup groupToEdit) {
        super(Component.literal(groupToEdit == null ? "Add New Group" : "Edit Group"));
        this.parent = parent;
        this.groupToEdit = groupToEdit;
        this.imageWidth = 250;
        this.imageHeight = 83;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        groupNameField = new EditBox(this.font, leftPos + 10, topPos + 20, 200, 20, Component.literal("Group Name"));
        if (groupToEdit != null) {
            groupNameField.setValue(groupToEdit.getName());
        }
        this.addRenderableWidget(groupNameField);

        this.addRenderableWidget(new ExtendedButton(leftPos + 10, topPos + 50, 50, 20, Component.literal(groupToEdit == null ? "Add" : "Save"), button -> {
            if (groupToEdit == null) {
                addGroup();
            } else {
                editGroup();
            }
            parent.saveGroups();
        }));

        this.addRenderableWidget(new ExtendedButton(leftPos + 70, topPos + 50, 50, 20, Component.literal("Cancel"), button -> {
            this.minecraft.setScreen(this.parent);
        }));
    }

    private void addGroup() {
        String groupName = groupNameField.getValue();
        if (!groupName.isEmpty()) {
            int newId = getNewID(GroupManageScreen.groups);
            RecruitsGroup newGroup = new RecruitsGroup(newId, groupName, false);
            GroupManageScreen.groups.add(newGroup);
            this.parent.setList = false;
            this.minecraft.setScreen(this.parent);
        }
    }

    private int getNewID(List<RecruitsGroup> groups) {
        int newId = 0;

        for (RecruitsGroup group: groups){
            if(group.getId() > newId){
                newId = group.getId();
            }
        }

        return newId + 1;
    }

    private void editGroup() {
        String newName = groupNameField.getValue();
        if (!newName.isEmpty() && groupToEdit != null) {
            RecruitsGroup copy = groupToEdit;
            GroupManageScreen.groups.remove(groupToEdit);
            copy.setName(newName);
            GroupManageScreen.groups.add(copy);

            this.parent.setList = false;
            this.minecraft.setScreen(this.parent);
        }
    }

    protected void render(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(RESOURCE_LOCATION, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

