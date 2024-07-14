package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.gui.group.RecruitsGroupButton;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/command_gui.png");

    private static final MutableComponent TOOLTIP_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.tooltip.strategic_fire");
    private static final MutableComponent TOOLTIP_DISMOUNT = Component.translatable("gui.recruits.command.tooltip.dismount");
    private static final MutableComponent TOOLTIP_MOUNT = Component.translatable("gui.recruits.command.tooltip.mount");
    private static final MutableComponent TOOLTIP_SHIELDS = Component.translatable("gui.recruits.command.tooltip.shields");
    private static final MutableComponent TOOLTIP_PROTECT = Component.translatable("gui.recruits.command.tooltip.protect");
    private static final MutableComponent TOOLTIP_MOVE = Component.translatable("gui.recruits.command.tooltip.move");
    private static final MutableComponent TOOLTIP_FOLLOW = Component.translatable("gui.recruits.command.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = Component.translatable("gui.recruits.command.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = Component.translatable("gui.recruits.command.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = Component.translatable("gui.recruits.command.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = Component.translatable("gui.recruits.command.tooltip.backToPos");
	private static final MutableComponent TOOLTIP_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.tooltip.backToMount");
    private static final MutableComponent TOOLTIP_PASSIVE = Component.translatable("gui.recruits.command.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = Component.translatable("gui.recruits.command.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = Component.translatable("gui.recruits.command.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = Component.translatable("gui.recruits.command.tooltip.raid");
    private static final MutableComponent TOOLTIP_UPKEEP = Component.translatable("gui.recruits.command.tooltip.upkeep");
    private static final MutableComponent TOOLTIP_TEAM = Component.translatable("gui.recruits.command.tooltip.team");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.command.tooltip.clearTargets");
    private static final MutableComponent TEXT_EVERYONE = Component.translatable("gui.recruits.command.text.everyone");
    private static final MutableComponent TEXT_PROTECT = Component.translatable("gui.recruits.command.text.protect");
    private static final MutableComponent TEXT_MOVE = Component.translatable("gui.recruits.command.text.move");
    private static final MutableComponent TEXT_SHIELDS = Component.translatable("gui.recruits.command.text.shields");
    private static final MutableComponent TEXT_DISMOUNT = Component.translatable("gui.recruits.command.text.dismount");
    private static final MutableComponent TEXT_MOUNT = Component.translatable("gui.recruits.command.text.mount");
    private static final MutableComponent TEXT_FOLLOW = Component.translatable("gui.recruits.command.text.follow");
    private static final MutableComponent TEXT_WANDER = Component.translatable("gui.recruits.command.text.wander");
    private static final MutableComponent TEXT_HOLD_MY_POS = Component.translatable("gui.recruits.command.text.holdMyPos");
    private static final MutableComponent TEXT_HOLD_POS = Component.translatable("gui.recruits.command.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = Component.translatable("gui.recruits.command.text.backToPos");
	private static final MutableComponent TEXT_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.text.backToMount");
    private static final MutableComponent TEXT_PASSIVE = Component.translatable("gui.recruits.command.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = Component.translatable("gui.recruits.command.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = Component.translatable("gui.recruits.command.text.aggressive");
    private static final MutableComponent TEXT_RAID = Component.translatable("gui.recruits.command.text.raid");
    private static final MutableComponent TEXT_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.text.strategic_fire");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.command.text.clearTargets");
    private static final MutableComponent TEXT_UPKEEP = Component.translatable("gui.recruits.command.text.upkeep");
    private static final MutableComponent TEXT_TEAM = Component.translatable("gui.recruits.command.text.team");
    private static final int fontColor = 16250871;
    private final Player player;
    private BlockPos rayBlockPos;
    private Entity rayEntity;
    private Selection selection;
    public static List<RecruitsGroup> groups;
    private List<RecruitsGroupButton> groupButtons;

    public CommandScreen(CommandMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }
    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x, y, z);
        if(!RecruitsClientConfig.CommandScreenToggle.get())this.onClose();
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        this.saveGroups();
        groups = new ArrayList<>();
        removeGroupButton = null;
        groupButtons = new ArrayList<>();
    }

    @Override
    public boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_) {
        if(p_94688_ > 0) this.setSelection(selection.getNext());
        else this.setSelection(selection.getBefore());
        return super.mouseScrolled(p_94686_, p_94687_, p_94688_);
    }

    @Override
    protected void init() {
        super.init();

        this.rayBlockPos = getBlockPos();
        this.rayEntity = ClientEvent.getEntityByLooking();

        selection = Selection.MOVEMENT;

    }
    private boolean buttonsSet = false;
    private ExtendedButton removeGroupButton;
    @Override
    protected void containerTick() {
        super.containerTick();
        if(groups != null && !groups.isEmpty() && !buttonsSet){
            this.setButtons();
            this.saveGroups();
            this.buttonsSet = true;
        }

        if(removeGroupButton != null && !removeGroupButton.isHovered()) {
            this.removeWidget(removeGroupButton);
            removeGroupButton = null;
        }
    }

    private void saveGroups() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups, true));
    }

    private int zeroLeftPos;
    private int zeroTopPos;
    private void setButtons(){
        clearWidgets();
        groupButtons = new ArrayList<>();
        zeroLeftPos = leftPos - 150;
        zeroTopPos = topPos - 40;
        //Group Buttons:

        int index = 0;
        for (RecruitsGroup group : groups) {
            createRecruitsGroupButton(group, index);
            index++;
        }
        createManageGroupsButton(index);

        // Other Buttons
    }

    private void createRecruitsGroupButton(RecruitsGroup group, int index) {
        RecruitsGroupButton groupButton = new RecruitsGroupButton(group,zeroLeftPos + 50 * index, zeroTopPos + 10, 40, 40, Component.literal(group.getName()),
        button -> {
            group.setDisabled(!group.isDisabled());
            this.setButtons();
        });
        addRenderableWidget(groupButton);
        groupButton.active = !group.isDisabled();

        this.groupButtons.add(groupButton);
    }

    private void createManageGroupsButton(int index){
        ExtendedButton groupButton = new ExtendedButton(zeroLeftPos + 50 * index, zeroTopPos + 30, 20, 20, Component.literal("+/-"),
                button -> {
                    CommandEvents.openGroupManageScreen(player);

                });
        addRenderableWidget(groupButton);
    }

    private void setSelection(Selection selection){
        this.selection = selection;
        this.setButtons();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
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


    private enum Selection{
        COMBAT((byte)0),
        MOVEMENT((byte)1),

        OTHER((byte)2);

        private final byte index;
        Selection(byte index){
            this.index = index;
        }

        public byte getIndex(){
            return this.index;
        }

        public Selection getNext(){
            int length = values().length;
            byte newIndex = (byte) (this.index + 1);
            if(newIndex >= length){
                return MOVEMENT;
            }
            else
                return fromIndex(newIndex);
        }

        public Selection getBefore() {
            int length = values().length;
            byte newIndex = (byte) (this.index - 1);
            if (newIndex < 0) {
                return values()[length - 1];
            } else {
                return fromIndex(newIndex);
            }
        }
        public static Selection fromIndex(byte index) {
            for (Selection state : Selection.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Selection index: " + index);
        }
    }
}
