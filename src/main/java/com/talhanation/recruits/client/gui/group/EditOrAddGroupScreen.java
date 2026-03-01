package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.ConfirmScreen;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.ImageSelectionDropdownMatrix;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import static com.talhanation.recruits.client.gui.RecruitMoreScreen.TOOLTIP_ASSIGN_GROUP_TO_PLAYER;
import static com.talhanation.recruits.client.gui.RecruitMoreScreen.TOOLTIP_KEEP_TEAM;

public class EditOrAddGroupScreen extends Screen {

    private static final int fontColor = 4210752;
    private EditBox groupNameField;
    private final RecruitsGroupListScreen parent;
    private final RecruitsGroup groupToEdit;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/gui_big.png");
    private static final MutableComponent TEXT_CANCEL = Component.translatable("gui.recruits.groups.cancel");
    private static final MutableComponent TEXT_SAVE = Component.translatable("gui.recruits.groups.save");
    private static final MutableComponent TEXT_ADD = Component.translatable("gui.recruits.groups.add");
    private static final MutableComponent TEXT_SPLIT = Component.translatable("gui.recruits.groups.split");
    private static final MutableComponent TEXT_EDIT_TITLE = Component.translatable("gui.recruits.groups.edit_title");
    private static final MutableComponent TEXT_ADD_TITLE = Component.translatable("gui.recruits.groups.add_title");
    private static final MutableComponent DISBAND_GROUP = Component.translatable("gui.recruits.inv.text.disbandGroup");
    private static final MutableComponent TOOLTIP_DISBAND_GROUP = Component.translatable("gui.recruits.inv.tooltip.disbandGroup");
    private static final MutableComponent ASSIGN_GROUP_TO_PLAYER = Component.translatable("gui.recruits.team.assignNewOwner");
    private static final MutableComponent BUTTON_MERGE = Component.translatable("gui.recruits.groups.merge");
    private static final MutableComponent BUTTON_SPLIT = Component.translatable("gui.recruits.groups.split");
    private static final MutableComponent TITLE_MERGE_GROUP = Component.translatable("gui.recruits.groups.merge_title");
    private static final MutableComponent TOOLTIP_MERGE_GROUP = Component.translatable("gui.recruits.groups.tooltip.merge");
    private static final MutableComponent BUTTON_NEARBY = Component.translatable("gui.recruits.groups.nearby");
    private static final MutableComponent TOOLTIP_PUT_NEARBY = Component.translatable("gui.recruits.groups.tooltip.nearby");
    private ImageSelectionDropdownMatrix imageDropdownMatrix;
    private ResourceLocation image;
    private final Player player;

    public EditOrAddGroupScreen(RecruitsGroupListScreen parent) {
        this(parent, null);
    }

    public EditOrAddGroupScreen(RecruitsGroupListScreen parent, RecruitsGroup groupToEdit) {
        super(Component.literal(""));
        this.player = Minecraft.getInstance().player;
        this.parent = parent;
        this.groupToEdit = groupToEdit;
        this.imageWidth = 195;
        this.imageHeight = 160;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;


        setWidgets();
    }

    public void setWidgets(){
        clearWidgets();

        groupNameField = new EditBox(this.font, leftPos + 7, topPos + 20, 150, 20, Component.literal(""));
        if (groupToEdit != null) {
            groupNameField.setValue(groupToEdit.getName());
        }
        this.addRenderableWidget(groupNameField);

        int index = groupToEdit != null ? groupToEdit.getImage() : 0;
        image = RecruitsGroup.IMAGES.get(index);
        imageDropdownMatrix = new ImageSelectionDropdownMatrix(this, leftPos + 170, topPos + 20, 21, 21,
                RecruitsGroup.IMAGES,
                this::setGroupImage
        );
        addRenderableWidget(imageDropdownMatrix);

        Button buttonDisbandGroup = new ExtendedButton(leftPos + 7, topPos + 50, 180, 20, DISBAND_GROUP,
            btn -> {
                minecraft.setScreen(new ConfirmScreen(DISBAND_GROUP, TOOLTIP_KEEP_TEAM,
                        () ->  Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.groupToEdit.getUUID(), true)),
                        () ->  Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.groupToEdit.getUUID(), false)),
                        () ->  minecraft.setScreen(EditOrAddGroupScreen.this)
                ));
            }
        );
        buttonDisbandGroup.setTooltip(Tooltip.create(TOOLTIP_DISBAND_GROUP));
        buttonDisbandGroup.active = groupToEdit != null;
        addRenderableWidget(buttonDisbandGroup);

        Button buttonAssignGroup = new ExtendedButton(leftPos + 7, topPos + 70, 180, 20, ASSIGN_GROUP_TO_PLAYER,
                btn -> {
                    minecraft.setScreen(new SelectPlayerScreen(this, player, ASSIGN_GROUP_TO_PLAYER, ASSIGN_GROUP_TO_PLAYER, TOOLTIP_ASSIGN_GROUP_TO_PLAYER, false, PlayersList.FilterType.NONE,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignGroupToPlayer(this.player.getUUID(), playerInfo, this.groupToEdit.getUUID()));
                            onClose();
                        } )
                    );
                }
        );
        buttonAssignGroup.active = groupToEdit != null;
        addRenderableWidget(buttonAssignGroup);

        Button mergeButton = new ExtendedButton(leftPos + 7, topPos + 90, 90, 20, BUTTON_MERGE,
            btn -> {
                minecraft.setScreen(new SelectGroupScreen(this, groupToEdit, TITLE_MERGE_GROUP, BUTTON_MERGE, TOOLTIP_MERGE_GROUP,
                        (selectedGroup) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageMergeGroup(this.groupToEdit.getUUID(), selectedGroup.getUUID()));
                            minecraft.setScreen(this.parent);
                        }));
            }
        );
        mergeButton.setTooltip(Tooltip.create(TOOLTIP_MERGE_GROUP));
        mergeButton.active = groupToEdit != null;
        addRenderableWidget(mergeButton);

        Button splitButton = new ExtendedButton(leftPos + 97, topPos + 90, 90, 20, BUTTON_SPLIT,
            btn -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageSplitGroup(this.groupToEdit.getUUID()));
                minecraft.setScreen(this.parent);
            }
        );
        splitButton.active = groupToEdit != null;
        addRenderableWidget(splitButton);

        Button putRecruits = new ExtendedButton(leftPos + 7, topPos + 110, 180, 20, BUTTON_NEARBY,
            btn -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignNearbyRecruitsInGroup(this.groupToEdit.getUUID()));
                minecraft.setScreen(this.parent);
            }
        );
        putRecruits.setTooltip(Tooltip.create(TOOLTIP_PUT_NEARBY));
        putRecruits.active = groupToEdit != null;
        addRenderableWidget(putRecruits);

        this.addRenderableWidget(new ExtendedButton(leftPos + 7, topPos + 135, 90, 20, groupToEdit == null ? TEXT_ADD : TEXT_SAVE, button -> {
            if (groupToEdit == null) {
                addGroup();
            } else {
                editGroup();
            }
        }));

        this.addRenderableWidget(new ExtendedButton(leftPos + 97, topPos + 135, 90, 20, TEXT_CANCEL, button -> {
            this.minecraft.setScreen(this.parent);
        }));
    }

    private void setGroupImage(ResourceLocation resourceLocation){
        this.image = resourceLocation;
        if(groupToEdit != null){
            int index = RecruitsGroup.IMAGES.indexOf(resourceLocation);
            this.groupToEdit.setImage(index);
        }
    }

    private void addGroup() {
        String groupName = groupNameField.getValue();
        if (!groupName.isEmpty()) {
            int image = RecruitsGroup.IMAGES.indexOf(this.image);
            RecruitsGroup newGroup = new RecruitsGroup(groupName, ClientManager.getPlayerInfo(), image);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateGroup(newGroup));
            ClientManager.groups.add(newGroup);

            this.minecraft.setScreen(this.parent);
        }
    }

    private void editGroup() {
        String newName = groupNameField.getValue();
        if (!newName.isEmpty() && groupToEdit != null) {
            groupToEdit.setName(newName);

            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateGroup(groupToEdit));

            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void tick() {
        super.tick();
        groupNameField.tick();
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(imageDropdownMatrix != null) imageDropdownMatrix.onMouseMove(x,y);
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int p_94697_) {
        if(imageDropdownMatrix != null) imageDropdownMatrix.onMouseClicked(x,y);

        return super.mouseClicked(x, y, p_94697_);
    }

    private void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, groupToEdit == null? TEXT_ADD_TITLE : TEXT_EDIT_TITLE, leftPos + 7, topPos + 5, fontColor, false);
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

    public ResourceLocation getSelectedImage() {
        return this.image;
    }
}

