package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class EditOrAddGroupScreen extends Screen {

    private static final int fontColor = 4210752;
    private EditBox groupNameField;
    private final GroupManageScreen parent;
    private RecruitsGroup groupToEdit;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_main_gui.png");
    private static final MutableComponent TEXT_CANCEL = Component.translatable("gui.recruits.group_creation.cancel");
    private static final MutableComponent TEXT_SAVE = Component.translatable("gui.recruits.group_creation.save");
    private static final MutableComponent TEXT_ADD = Component.translatable("gui.recruits.group_creation.add");
    private static final MutableComponent TEXT_SPLIT = Component.translatable("gui.recruits.group_creation.split");
    private static final MutableComponent TEXT_EDIT_TITLE = Component.translatable("gui.recruits.group_creation.edit_title");
    private static final MutableComponent TEXT_ADD_TITLE = Component.translatable("gui.recruits.group_creation.add_title");
    public EditOrAddGroupScreen(GroupManageScreen parent) {
        this(parent, null);
    }

    public EditOrAddGroupScreen(GroupManageScreen parent, RecruitsGroup groupToEdit) {
        super(Component.literal(""));
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

        groupNameField = new EditBox(this.font, leftPos + 10, topPos + 20, 220, 20, Component.literal(""));
        if (groupToEdit != null) {
            groupNameField.setValue(groupToEdit.getName());
        }
        this.addRenderableWidget(groupNameField);

        this.addRenderableWidget(new ExtendedButton(leftPos + 10, topPos + 55, 60, 20, groupToEdit == null ? TEXT_ADD : TEXT_SAVE, button -> {
            if (groupToEdit == null) {
                addGroup();
            } else {
                editGroup();
            }
            parent.saveGroups();
        }));

        this.addRenderableWidget(new ExtendedButton(leftPos + 170, topPos + 55, 60, 20, TEXT_CANCEL, button -> {
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

    @Override
    public void tick() {
        super.tick();
        groupNameField.tick();
    }

    private void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, groupToEdit == null? TEXT_ADD_TITLE : TEXT_EDIT_TITLE, leftPos + 10  , topPos + 5, fontColor, false);
    }
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        guiGraphics.blit(RESOURCE_LOCATION, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderForeground(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

