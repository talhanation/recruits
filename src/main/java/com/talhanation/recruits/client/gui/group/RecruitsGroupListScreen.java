package com.talhanation.recruits.client.gui.group;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.widgets.ListScreenBase;
import com.talhanation.recruits.network.MessageApplyNoGroup;
import com.talhanation.recruits.network.MessageToServerRequestUpdateGroupList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class RecruitsGroupListScreen extends ListScreenBase {

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/select_player.png");
    protected static final Component TITLE = Component.translatable("gui.recruits.groups.title");
    protected static final Component ADD_BUTTON = Component.translatable("gui.recruits.groups.add");
    protected static final Component EDIT_BUTTON = Component.translatable("gui.recruits.groups.edit");
    protected static final Component REMOVE_BUTTON = Component.translatable("gui.recruits.groups.remove");
    protected static final int HEADER_SIZE = 16;
    protected static final int FOOTER_SIZE = 32;
    protected static final int SEARCH_HEIGHT = 16;
    protected static final int UNIT_SIZE = 18;
    protected static final int CELL_HEIGHT = 36;

    protected RecruitsGroupList groupList;
    protected EditBox searchBox;
    protected String lastSearch;
    protected int units;

    protected Screen parent;
    private RecruitsGroup selected;
    private Button editButton;
    private Button removeButton;
    private final Player player;
    private int gapTop;
    private int gapBottom;

    public RecruitsGroupListScreen(Player player){
        super(TITLE,236,0);
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateGroupList());

        gapTop = (int) (this.height * 0.1);
        gapBottom = (int) (this.height * 0.1);

        guiLeft = guiLeft + 2;
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT + SEARCH_HEIGHT + 4) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom - SEARCH_HEIGHT) / UNIT_SIZE);

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        if (groupList != null) {
            groupList.updateSize(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE);
        } else {
            groupList = new RecruitsGroupList(width, height, guiTop + HEADER_SIZE + SEARCH_HEIGHT, guiTop + HEADER_SIZE + units * UNIT_SIZE, CELL_HEIGHT, this);
        }
        String string = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, guiLeft + 8, guiTop + HEADER_SIZE, 220, SEARCH_HEIGHT, Component.literal(""));
        searchBox.setMaxLength(16);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(string);
        searchBox.setResponder(this::checkSearchStringUpdate);
        addWidget(searchBox);
        addWidget(groupList);

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;

        addRenderableWidget(createAddGroupButton(guiLeft + 7, buttonY));

        this.editButton =  createEditGroupButton(guiLeft + 87, buttonY);
        this.editButton.active = this.selected != null;
        addRenderableWidget(this.editButton);

        this.removeButton = createRemoveGroupButton(guiLeft + 167, buttonY);
        this.removeButton.active = this.selected != null;
        addRenderableWidget(this.removeButton);
    }

    @Override
    public void tick() {
        super.tick();
        if(searchBox != null){
            searchBox.tick();
        }
        if(groupList != null){
            groupList.tick();
        }
    }

    private Button createRemoveGroupButton(int x, int y) {
        return new Button(x, y, 60, 20, REMOVE_BUTTON, button -> {
            if (selected != null) {

                Main.SIMPLE_CHANNEL.sendToServer(new MessageApplyNoGroup(player.getUUID(), selected.getId()));

                RecruitsGroupList.groups.remove(selected);

                RecruitsGroupList.saveGroups(false);

                this.init();
            }
        });
    }

    private Button createAddGroupButton(int x, int y) {
        return new Button(x, y, 60, 20, ADD_BUTTON, btn -> {
            this.minecraft.setScreen(new EditOrAddGroupScreen(this));
        });
    }

    private Button createEditGroupButton(int x, int y) {
        return new Button(x, y, 60, 20, EDIT_BUTTON, btn -> {
            if(selected != null) this.minecraft.setScreen(new EditOrAddGroupScreen(this, selected));
        });
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        boolean flag = super.keyPressed(p_96552_, p_96553_, p_96554_);
        this.selected = null;
        this.groupList.setFocused(null);
        this.editButton.active = false;
        this.removeButton.active = false;

        return flag;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, HEADER_SIZE);
        for (int i = 0; i < units; i++) {
            blit(poseStack, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * i, 0, HEADER_SIZE, xSize, UNIT_SIZE);
        }
        blit(poseStack, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * units, 0, HEADER_SIZE + UNIT_SIZE, xSize, FOOTER_SIZE);
        blit(poseStack, guiLeft + 10, guiTop + HEADER_SIZE + 6 - 2, xSize, 0, 12, 12);
    }

    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        font.draw(poseStack, this.getTitle(), width / 2 - font.width(TITLE) / 2, guiTop + 5, 4210752);

        if (!groupList.isEmpty()) {
            groupList.render(poseStack, mouseX, mouseY, delta);
        } else if (!searchBox.getValue().isEmpty()) {
            drawCenteredString(poseStack, font, "EMPTY_SEARCH", width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
        }
        if (!searchBox.isFocused() && searchBox.getValue().isEmpty()) {
            drawString(poseStack, font, "", searchBox.x, searchBox.y, -1);
        }
        searchBox.render(poseStack, mouseX, mouseY, delta);
    }

    private void checkSearchStringUpdate(String string) {
        if (!(string = string.toLowerCase(Locale.ROOT)).equals(lastSearch)) {
            groupList.setFilter(string);
            lastSearch = string;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        if(groupList != null) groupList.mouseClicked(x,y,z);
        boolean flag = super.mouseClicked(x, y, z);
        if(this.groupList.getFocused() != null){
            this.selected = this.groupList.getFocused().getGroup();
            this.editButton.active = selected.id != 0;
            this.removeButton.active =  selected.id != 0;
        }

        return flag;
    }

    public RecruitsGroup getSelected(){
        return this.selected;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

}
