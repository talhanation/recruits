package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.*;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/command_gui.png");
    private static final MutableComponent TEXT_EVERYONE = Component.translatable("gui.recruits.command.text.everyone");
    private static final int fontColor = 16250871;
    public final Player player;
    public BlockPos rayBlockPos;
    public Entity rayEntity;
    private ICommandCategory currentCategory;
    public static List<RecruitsGroup> groups;
    public static Formation formation;
    public boolean mouseGroupsInverted;
    private List<RecruitsGroupButton> groupButtons;

    public CommandScreen(CommandMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        player = playerInventory.player;
    }
    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x, y, z);
        if(!RecruitsClientConfig.CommandScreenToggle.get()) this.onClose();
        return true;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> @NotNull T addRenderableWidget(@NotNull T widget) {
        this.renderables.add(widget);
        return this.addWidget(widget);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.saveGroups();
        groups = new ArrayList<>();
        groupButtons = new ArrayList<>();
        this.saveCategoryOnClient();
    }

    @Override
    protected void init() {
        super.init();
        this.rayBlockPos = getBlockPos();
        this.rayEntity = ClientEvent.getEntityByLooking();
        this.currentCategory = getSelectionFromClient();
        formation = getSavedFormationFromClient();
    }

    private boolean buttonsSet = false;

    @Override
    protected void containerTick() {
        super.containerTick();
        if(!buttonsSet){
            this.setButtons();
            this.saveGroups();
            this.buttonsSet = true;
        }
    }

    private void saveGroups() {
        if(groups != null && !groups.isEmpty()){
            Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups, true));
        }
    }
    boolean statusSet = false;
    private void setButtons(){
        int x = this.width / 2;
        int y = this.height / 2;
        formation = this.getSavedFormationFromClient();
        clearWidgets();
        groupButtons = new ArrayList<>();
        int index = 0;
        if(groups != null && !groups.isEmpty()){

            for (RecruitsGroup group : groups) {
                if( index < 9){
                    createRecruitsGroupButton(group, index, x, y);
                    index++;
                }
            }
        }
        createManageGroupsButton(index, x, y);

        createCategoryButtons(x,y);

        currentCategory.createButtons(this, x, y, groups, player);

        if(!statusSet){
            this.mouseGroupsInverted = getInvertedStatus();
            statusSet = true;
        }

    }

    private void createRecruitsGroupButton(RecruitsGroup group, int index, int x, int y) {
        RecruitsGroupButton groupButton = new RecruitsGroupButton(group,x - 200 + 45 * index, y - 120, 40, 40, Component.literal(group.getName()),
        button -> {
            group.setDisabled(!group.isDisabled());
            this.setButtons();
        });
        addRenderableWidget(groupButton);
        groupButton.active = !group.isDisabled();

        this.groupButtons.add(groupButton);
    }

    private void createManageGroupsButton(int index, int x, int y){
        int posX = x - 200 + 45 * index;
        int posY = y - 100;

        if(index > 8){
            posX = x + 180;
            posY = y - 70;
        }

        ExtendedButton groupButton = new ExtendedButton(posX, posY, 20, 20, Component.literal("+/-"),
                button -> {
                    minecraft.setScreen(new RecruitsGroupListScreen(player));

                });
        addRenderableWidget(groupButton);
    }

    private void setCurrentCategory(ICommandCategory currentCategory){
        this.currentCategory = currentCategory;
        this.saveCategoryOnClient();
        this.setButtons();
    }

    public void setFormation(Formation f){
        formation = f;
        this.saveFormationSelection();
        this.setButtons();
    }

    private ICommandCategory getSelectionFromClient() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        byte x = nbt.getByte("RecruitsCategory");
        return CommandCategoryManager.getByIndex(x);
    }
    private void saveCategoryOnClient() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt("RecruitsCategory", CommandCategoryManager.getCategories().indexOf(currentCategory));
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private void createCategoryButtons(int centerX, int centerY) {
        List<ICommandCategory> allCategories = CommandCategoryManager.getCategories();
        int spacing = 21;
        int count = allCategories.size();

        int totalWidth = (count - 1) * spacing;
        int startX = centerX - totalWidth / 2;

        int buttonY = centerY + 85;

        for (int i = 0; i < count; i++) {
            ICommandCategory category = allCategories.get(i);
            int x = startX + i * spacing;

            RecruitsCategoryButton button = new RecruitsCategoryButton(
                    category.getIcon(), x, buttonY, Component.literal(""),
                    press -> this.setCurrentCategory(category)
            );

            button.setTooltip(Tooltip.create(category.getToolTipName()));
            button.active = category == this.currentCategory;
            addRenderableWidget(button);
        }
    }



    public void sendMovementCommandToServer(int state) {
        if(state != 1){
            Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveFormationFollowMovement(player.getUUID(), new int[]{}, -1));
        }
        if(!groups.isEmpty()){
            for(RecruitsGroup group : groups){
                if(!group.isDisabled()) Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), state, group.getId(), formation.getIndex()));
            }
        }
    }

    public Formation getSavedFormationFromClient() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return Formation.fromIndex((byte) nbt.getInt("FormationSelection"));
    }

    public void saveFormationSelection() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putByte("FormationSelection", formation.getIndex());
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public void sendCommandInChat(int state){
        StringBuilder group_string = new StringBuilder();
        int i = 0;

        for(RecruitsGroup group : groups){
            if(!group.isDisabled()) i++;
        }

        if (i >= 9){
            group_string = new StringBuilder(TEXT_EVERYONE.getString() + ", ");
        }
        else {
           for(RecruitsGroup group : groups){
               if(!group.isDisabled()) group_string.append(group.getName()).append(", ");
           }
        }

        switch (state) {
            case 0 -> this.player.sendSystemMessage(TEXT_WANDER(group_string.toString()));
            case 1 -> this.player.sendSystemMessage(TEXT_FOLLOW(group_string.toString()));
            case 2 -> this.player.sendSystemMessage(TEXT_HOLD_POS(group_string.toString()));
            case 3 -> this.player.sendSystemMessage(TEXT_BACK_TO_POS(group_string.toString()));
            case 4 -> this.player.sendSystemMessage(TEXT_HOLD_MY_POS(group_string.toString()));
            case 5 -> this.player.sendSystemMessage(TEXT_PROTECT(group_string.toString()));
            case 6 -> this.player.sendSystemMessage(TEXT_MOVE(group_string.toString()));
            case 7 -> this.player.sendSystemMessage(TEXT_FORWARD(group_string.toString()));
            case 8 -> this.player.sendSystemMessage(TEXT_BACKWARD(group_string.toString()));
            case 9 -> this.player.sendSystemMessage(TEXT_CLEAR_TARGETS(group_string.toString()));

            case 10 -> this.player.sendSystemMessage(TEXT_NEUTRAL(group_string.toString()));
            case 11 -> this.player.sendSystemMessage(TEXT_AGGRESSIVE(group_string.toString()));
            case 12 -> this.player.sendSystemMessage(TEXT_RAID(group_string.toString()));
            case 13 -> this.player.sendSystemMessage(TEXT_PASSIVE(group_string.toString()));

            case 70 -> this.player.sendSystemMessage(TEXT_FIRE_AT_WILL(group_string.toString()));
            case 71 -> this.player.sendSystemMessage(TEXT_HOLD_FIRE(group_string.toString()));
            case 72 -> this.player.sendSystemMessage(TEXT_STRATEGIC_FIRE(group_string.toString()));
            case 73 -> this.player.sendSystemMessage(TEXT_STRATEGIC_FIRE_OFF(group_string.toString()));
            case 74 -> this.player.sendSystemMessage(TEXT_SHIELDS(group_string.toString()));
            case 75 -> this.player.sendSystemMessage(TEXT_SHIELDS_OFF(group_string.toString()));

            case 88 -> this.player.sendSystemMessage(TEXT_REST(group_string.toString()));
            case 91 -> this.player.sendSystemMessage(TEXT_BACK_TO_MOUNT(group_string.toString()));
            case 92 -> this.player.sendSystemMessage(TEXT_UPKEEP(group_string.toString()));
            case 93 -> this.player.sendSystemMessage(TEXT_CLEAR_UPKEEP(group_string.toString()));

            case 98 -> this.player.sendSystemMessage(TEXT_DISMOUNT(group_string.toString()));
            case 99 -> this.player.sendSystemMessage(TEXT_MOUNT(group_string.toString()));
        }
    }
    private static MutableComponent TEXT_WANDER(String group_string) {
        return Component.translatable("chat.recruits.command.wander", group_string);
    }

    private static MutableComponent TEXT_FOLLOW(String group_string) {
        return Component.translatable("chat.recruits.command.follow", group_string);
    }

    private static MutableComponent TEXT_HOLD_POS(String group_string) {
        return Component.translatable("chat.recruits.command.holdPos", group_string);
    }

    private static MutableComponent TEXT_BACK_TO_POS(String group_string) {
        return Component.translatable("chat.recruits.command.backToPos", group_string);
    }

    private static MutableComponent TEXT_BACK_TO_MOUNT(String group_string) {
        return Component.translatable("chat.recruits.command.backToMount", group_string);
    }

    private static MutableComponent TEXT_REST(String group_string) {
        return Component.translatable("chat.recruits.command.rest", group_string);
    }
    private static MutableComponent TEXT_HOLD_MY_POS(String group_string) {
        return Component.translatable("chat.recruits.command.holdMyPos", group_string);
    }

    private static MutableComponent TEXT_PROTECT(String group_string) {
        return Component.translatable("chat.recruits.command.protect", group_string);
    }

    private static MutableComponent TEXT_UPKEEP(String group_string) {
        return Component.translatable("chat.recruits.command.upkeep", group_string);
    }
    private static MutableComponent TEXT_CLEAR_UPKEEP(String group_string) {
        return Component.translatable("chat.recruits.command.clear_upkeep", group_string);
    }
    private static MutableComponent TEXT_SHIELDS_OFF(String group_string) {
        return Component.translatable("chat.recruits.command.shields_off", group_string);
    }

    private static MutableComponent TEXT_STRATEGIC_FIRE_OFF(String group_string) {
        return Component.translatable("chat.recruits.command.strategic_fire_off", group_string);
    }

    private static MutableComponent TEXT_SHIELDS(String group_string) {
        return Component.translatable("chat.recruits.command.shields", group_string);
    }

    private static MutableComponent TEXT_STRATEGIC_FIRE(String group_string) {
        return Component.translatable("chat.recruits.command.strategic_fire", group_string);
    }

    private static MutableComponent TEXT_MOVE(String group_string) {
        return Component.translatable("chat.recruits.command.move", group_string);
    }

    private static MutableComponent TEXT_FORWARD(String group_string) {
        return Component.translatable("chat.recruits.command.forward", group_string);
    }
    private static MutableComponent TEXT_BACKWARD(String group_string) {
        return Component.translatable("chat.recruits.command.backward", group_string);
    }

    private static MutableComponent TEXT_CLEAR_TARGETS(String group_string) {
        return Component.translatable("chat.recruits.command.clearTargets", group_string);
    }
    private static MutableComponent TEXT_DISMOUNT(String group_string) {
        return Component.translatable("chat.recruits.command.dismount", group_string);
    }

    private static MutableComponent TEXT_MOUNT(String group_string) {
        return Component.translatable("chat.recruits.command.mount", group_string);
    }

    private static MutableComponent TEXT_PASSIVE(String group_string) {
        return Component.translatable("chat.recruits.command.passive", group_string);
    }

    private static MutableComponent TEXT_RAID(String group_string) {
        return Component.translatable("chat.recruits.command.raid", group_string);
    }

    private static MutableComponent TEXT_AGGRESSIVE(String group_string) {
        return Component.translatable("chat.recruits.command.aggressive", group_string);
    }

    private static MutableComponent TEXT_NEUTRAL(String group_string) {
        return Component.translatable("chat.recruits.command.neutral", group_string);
    }

    private static MutableComponent TEXT_SHIELDS_UP(String group_string) {
        return Component.translatable("chat.recruits.command.shields", group_string);
    }

    private static MutableComponent TEXT_SHIELDS_DOWN(String group_string) {
        return Component.translatable("chat.recruits.command.shields_off", group_string);
    }

    private static MutableComponent TEXT_FIRE_AT_WILL(String group_string) {
        return Component.translatable("chat.recruits.command.fire_at_will", group_string);
    }

    private static MutableComponent TEXT_HOLD_FIRE(String group_string) {
        return Component.translatable("chat.recruits.command.hold_fire", group_string);
    }

    private static MutableComponent TEXT_SELECT_ALL_GROUPS() {
        return Component.translatable("gui.recruits.command.tip.de_select_groups");
    }
    private static MutableComponent TEXT_SCROLL_CATEGORIES() {
        return Component.translatable("gui.recruits.command.tip.scrollCategories");
    }

    int xTipPos = 140;
    int yTipPos = 157;
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        String tipAllGroups = TEXT_SELECT_ALL_GROUPS().getString();
        String tipScroll = TEXT_SCROLL_CATEGORIES().getString();
        guiGraphics.drawString(font, tipAllGroups, xTipPos, yTipPos, FONT_COLOR, false);
        guiGraphics.drawString(font, tipScroll, xTipPos, yTipPos + 15, FONT_COLOR, false);

    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    }

    @Nullable
    private BlockPos getBlockPos(){
        HitResult rayTraceResult = player.pick(100, 1F, true);
        if (rayTraceResult != null) {
            if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockraytraceresult = (BlockHitResult) rayTraceResult;

                return blockraytraceresult.getBlockPos();
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double x, double y, int id) {
        if(id == 1){
            this.invertGroups();
        }

        return super.mouseClicked(x, y, id);
    }

    @Override
    public boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_) {
        if(p_94688_ > 0){
            this.setCurrentCategory(CommandCategoryManager.getPrevious(currentCategory));
        }
        else{
            this.setCurrentCategory(CommandCategoryManager.getNext(currentCategory));
        }

        return super.mouseScrolled(p_94686_, p_94687_, p_94688_);
    }

    private void invertGroups() {
        for(RecruitsGroupButton button : groupButtons){
            button.getGroup().setDisabled(this.mouseGroupsInverted);
        }
        this.setButtons();
        this.mouseGroupsInverted = !this.mouseGroupsInverted;
    }

    private boolean getInvertedStatus() {
        boolean allActive = true;
        boolean allInactive = true;

        for (RecruitsGroupButton button : groupButtons) {
            if (button.active) {
                allInactive = false;
            } else {
                allActive = false;
            }

            if (!allActive && !allInactive) {
                return false;
            }
        }

        return allActive;
    }

    @OnlyIn(Dist.CLIENT)
    public enum Formation {
        NONE((byte) 0),
        LINE((byte) 1),
        SQUARE((byte) 2),
        TRIANGLE((byte) 3),
        HCIRCLE((byte) 4),
        HSQUARE((byte) 5),
        VFORM((byte) 6),
        CIRCLE((byte) 7),
        MOVEMENT((byte) 8);

        private final byte index;

        Formation(byte index) {
            this.index = index;
        }

        public byte getIndex() {
            return this.index;
        }


        public static Formation fromIndex(byte index) {
            for (Formation state : Formation.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Selection index: " + index);
        }
    }
}
