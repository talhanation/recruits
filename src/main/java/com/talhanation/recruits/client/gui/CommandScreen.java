package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.client.events.PlayerEvents;
import com.talhanation.recruits.client.gui.group.*;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
    private static final MutableComponent TOOLTIP_MOVE_HOLD = Component.translatable("gui.recruits.command.tooltip.move_hold");
    private static final MutableComponent TOOLTIP_FORWARD = Component.translatable("gui.recruits.command.tooltip.forward");
    private static final MutableComponent TOOLTIP_FORMATION = Component.translatable("gui.recruits.command.tooltip.formation");
    private static final MutableComponent TOOLTIP_BACKWARD = Component.translatable("gui.recruits.command.tooltip.backward");
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
    private static final MutableComponent TOOLTIP_REST = Component.translatable("gui.recruits.command.tooltip.rest");
    private static final MutableComponent TOOLTIP_TEAM = Component.translatable("gui.recruits.command.tooltip.team");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.command.tooltip.clearTargets");
    private static final MutableComponent TOOLTIP_HOLD_FIRE = Component.translatable("gui.recruits.command.tooltip.hold_fire");
    private static final MutableComponent TOOLTIP_FIRE_AT_WILL = Component.translatable("gui.recruits.command.tooltip.fire_at_will");
    private static final MutableComponent TOOLTIP_MOVEMENT = Component.translatable("gui.recruits.command.tooltip.movement");
    private static final MutableComponent TOOLTIP_COMBAT = Component.translatable("gui.recruits.command.tooltip.combat");
    private static final MutableComponent TOOLTIP_OTHER = Component.translatable("gui.recruits.command.tooltip.other");

    private static final MutableComponent TEXT_EVERYONE = Component.translatable("gui.recruits.command.text.everyone");
    private static final MutableComponent TEXT_PROTECT = Component.translatable("gui.recruits.command.text.protect");
    private static final MutableComponent TEXT_MOVE = Component.translatable("gui.recruits.command.text.move");
    private static final MutableComponent TEXT_MOVE_HOLD = Component.translatable("gui.recruits.command.text.move_hold");
    private static final MutableComponent TEXT_FORWARD = Component.translatable("gui.recruits.command.text.forward");
    private static final MutableComponent TEXT_BACKWARD = Component.translatable("gui.recruits.command.text.backward");
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
    private static final MutableComponent TEXT_HOLD_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.text.hold_strategic_fire");
    private static final MutableComponent TEXT_FIRE_AT_WILL = Component.translatable("gui.recruits.command.text.fire_at_will");
    private static final MutableComponent TEXT_HOLD_FIRE = Component.translatable("gui.recruits.command.text.hold_fire");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.command.text.clearTargets");
    private static final MutableComponent TEXT_UPKEEP = Component.translatable("gui.recruits.command.text.upkeep");
    private static final MutableComponent TEXT_REST = Component.translatable("gui.recruits.command.text.rest");
    private static final MutableComponent TEXT_TEAM = Component.translatable("gui.recruits.command.text.team");
    private static final MutableComponent TEXT_FORMATION_NONE = Component.translatable("gui.recruits.command.text.formation_none");
    private static final MutableComponent TEXT_FORMATION_LINEUP = Component.translatable("gui.recruits.command.text.formation_lineup");
    private static final MutableComponent TEXT_FORMATION_SQUARE = Component.translatable("gui.recruits.command.text.formation_square");
    private static final MutableComponent TEXT_FORMATION_TRIANGLE = Component.translatable("gui.recruits.command.text.formation_triangle");
    private static final MutableComponent TEXT_FORMATION_HOLLOW_SQUARE = Component.translatable("gui.recruits.command.text.formation_hollow_square");
    private static final MutableComponent TEXT_FORMATION_HOLLOW_CIRCLE = Component.translatable("gui.recruits.command.text.formation_hollow_circle");
    private static final MutableComponent TEXT_FORMATION_V = Component.translatable("gui.recruits.command.text.formation_v");
    private static final int fontColor = 16250871;
    private final Player player;
    private BlockPos rayBlockPos;
    private Entity rayEntity;
    private Selection selection;
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
        if(!RecruitsClientConfig.CommandScreenToggle.get())this.onClose();
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        this.saveGroups();
        groups = new ArrayList<>();
        groupButtons = new ArrayList<>();
        this.saveSelectionOnClient();
    }

    @Override
    protected void init() {
        super.init();
        this.rayBlockPos = getBlockPos();
        this.rayEntity = ClientEvent.getEntityByLooking();
        this.selection = getSelectionFromClient();
        formation = getSavedFormationFromClient();
    }

    private boolean buttonsSet = false;

    @Override
    protected void containerTick() {
        super.containerTick();
        if(groups != null && !groups.isEmpty() && !buttonsSet){
            this.setButtons();
            this.saveGroups();
            this.buttonsSet = true;
        }
    }

    private void saveGroups() {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups, true));
    }
    boolean statusSet = false;
    private void setButtons(){
        int x = this.width / 2;
        int y = this.height / 2;
        formation = this.getSavedFormationFromClient();
        clearWidgets();
        groupButtons = new ArrayList<>();

        int index = 0;
        for (RecruitsGroup group : groups) {
            if( index < 9){
                createRecruitsGroupButton(group, index, x, y);
                index++;
            }
        }
        createManageGroupsButton(index, x, y);

        createCommandButtons(selection, x, y);

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
                    CommandEvents.openGroupManageScreen(player);

                });
        addRenderableWidget(groupButton);
    }

    private void setSelection(Selection selection){
        this.selection = selection;
        this.saveSelectionOnClient();
        this.setButtons();
    }

    private void setFormation(Formation f){
        formation = f;
        this.saveFormationSelection();
        this.setButtons();
    }

    private Selection getSelectionFromClient() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        byte x = nbt.getByte("RecruitsSelection");
        return Selection.fromIndex(x);
    }
    private void saveSelectionOnClient() {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt("RecruitsSelection", selection.index);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    RecruitsCommandButton formationButton;
    private void createCommandButtons(Selection selection, int x, int y) {
        RecruitsCategoryButton movementButton = new RecruitsCategoryButton(Items.LEATHER_BOOTS.getDefaultInstance(), x , y + 87, Component.literal(""),
                button -> {
                    this.setSelection(Selection.MOVEMENT);
                });
        movementButton.setTooltip(Tooltip.create(TOOLTIP_MOVEMENT));
        addRenderableWidget(movementButton);

        RecruitsCategoryButton combatButton = new RecruitsCategoryButton(Items.IRON_SWORD.getDefaultInstance(), x - 30, y + 87, Component.literal(""),
                button -> {
                    this.setSelection(Selection.COMBAT);
                });
        combatButton.setTooltip(Tooltip.create(TOOLTIP_COMBAT));
        addRenderableWidget(combatButton);

        RecruitsCategoryButton otherButton = new RecruitsCategoryButton(Items.CHEST.getDefaultInstance(), x + 30, y + 87, Component.literal(""),
                button -> {
                    this.setSelection(Selection.OTHER);
                });
        otherButton.setTooltip(Tooltip.create(TOOLTIP_OTHER));
        addRenderableWidget(otherButton);


        movementButton.active = selection == Selection.MOVEMENT;
        combatButton.active = selection == Selection.COMBAT;
        otherButton.active = selection == Selection.OTHER;

        switch (selection){
            case MOVEMENT -> {
                //MOVE
                RecruitsCommandButton moveButton = new RecruitsCommandButton(x, y - 50, TEXT_MOVE,
                        button -> {
                            sendMovementCommandToServer(6);
                            sendMovementCommandInChat(6);
                        });
                moveButton.setTooltip(Tooltip.create(TOOLTIP_MOVE));
                addRenderableWidget(moveButton);

                //FORWARD
                RecruitsCommandButton forwardButton = new RecruitsCommandButton(x - 60, y - 25, TEXT_FORWARD,
                        button -> {
                            sendMovementCommandToServer(7);
                            sendMovementCommandInChat(7);
                });
                forwardButton.setTooltip(Tooltip.create(TOOLTIP_FORWARD));
                addRenderableWidget(forwardButton);

                //FOLLOW
                RecruitsCommandButton followButton = new RecruitsCommandButton(x + 60, y - 25, TEXT_FOLLOW,
                        button -> {
                            if(formation.getIndex() != 0){
                                PlayerEvents.activeGroups = groups;
                                PlayerEvents.followFormation = true;
                            }
                            else {
                                sendMovementCommandToServer(1);
                            }
                            sendMovementCommandInChat(1);
                        });
                followButton.setTooltip(Tooltip.create(TOOLTIP_FOLLOW));
                addRenderableWidget(followButton);


                //WANDER FREELY
                RecruitsCommandButton wanderButton = new RecruitsCommandButton(x + 120, y, TEXT_WANDER,
                        button -> {
                            sendMovementCommandToServer(0);
                            sendMovementCommandInChat(0);

                        });
                wanderButton.setTooltip(Tooltip.create(TOOLTIP_WANDER));
                addRenderableWidget(wanderButton);

                //BACK TO POS
                RecruitsCommandButton backToPosButton = new RecruitsCommandButton(x - 120, y, TEXT_BACK_TO_POS,
                        button -> {
                            sendMovementCommandToServer(3);
                            sendMovementCommandInChat(3);

                        });
                backToPosButton.setTooltip(Tooltip.create(TOOLTIP_BACK_TO_POS));
                addRenderableWidget(backToPosButton);

                //HOLDPOS
                RecruitsCommandButton holdPosButton = new RecruitsCommandButton(x + 60, y + 25, TEXT_HOLD_POS,
                        button -> {
                            sendMovementCommandToServer(2);
                            sendMovementCommandInChat(2);
                        });
                holdPosButton.setTooltip(Tooltip.create(TOOLTIP_HOLD_POS));
                addRenderableWidget(holdPosButton);

                //BACKWARD
                RecruitsCommandButton backwardButton = new RecruitsCommandButton(x - 60, y + 25, TEXT_BACKWARD,
                        button -> {
                            sendMovementCommandToServer(8);
                            sendMovementCommandInChat(8);
                        });
                backwardButton.setTooltip(Tooltip.create(TOOLTIP_BACKWARD));
                addRenderableWidget(backwardButton);

                //NONE
                RecruitsFormationButton noneFormationButton = new RecruitsFormationButton(Formation.NONE, x, y + 50,
                        button -> {
                            this.setFormation(Formation.NONE);
                        });
                noneFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_NONE));
                addRenderableWidget(noneFormationButton);

                //LINE UP
                RecruitsFormationButton lineUpFormationButton = new RecruitsFormationButton(Formation.LINE, x - 21, y + 50,
                        button -> {
                            this.setFormation(Formation.LINE);
                        });
                lineUpFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_LINEUP));
                addRenderableWidget(lineUpFormationButton);

                //SQUARE
                RecruitsFormationButton squareFormationButton = new RecruitsFormationButton(Formation.SQUARE, x + 21, y + 50,
                        button -> {
                            this.setFormation(Formation.SQUARE);
                        });
                squareFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_SQUARE));
                addRenderableWidget(squareFormationButton);

                //TRIANGLE
                RecruitsFormationButton triangleFormationButton = new RecruitsFormationButton(Formation.TRIANGLE, x - 42, y + 50,
                        button -> {
                            this.setFormation(Formation.TRIANGLE);
                        });
                triangleFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_TRIANGLE));
                addRenderableWidget(triangleFormationButton);

                //V_FORM
                RecruitsFormationButton VFormFormationButton = new RecruitsFormationButton(Formation.VFORM, x + 42, y + 50,
                        button -> {
                            this.setFormation(Formation.VFORM);
                        });
                VFormFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_V));
                addRenderableWidget(VFormFormationButton);

                //CIRCLE
                RecruitsFormationButton circleFormationButton = new RecruitsFormationButton(Formation.HCIRCLE, x - 63, y + 50,
                        button -> {
                            this.setFormation(Formation.HCIRCLE);
                        });
                circleFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_HOLLOW_CIRCLE));
                addRenderableWidget(circleFormationButton);

                //H SQAURE
                RecruitsFormationButton hSquareFormationButton = new RecruitsFormationButton(Formation.HSQUARE, x + 63, y + 50,
                        button -> {
                            this.setFormation(Formation.HSQUARE);
                        });
                hSquareFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_HOLLOW_SQUARE));
                addRenderableWidget(hSquareFormationButton);

                noneFormationButton.active = formation == Formation.NONE;
                lineUpFormationButton.active = formation == Formation.LINE;
                squareFormationButton.active = formation == Formation.SQUARE;
                hSquareFormationButton.active = formation == Formation.HSQUARE;
                triangleFormationButton.active = formation == Formation.TRIANGLE;
                VFormFormationButton.active = formation == Formation.VFORM;
                circleFormationButton.active = formation == Formation.HCIRCLE;
            }
            case COMBAT -> {
                //STRATEGIC FIRE
                RecruitsCommandButton strategicFireButton = new RecruitsCommandButton(x, y - 50, TEXT_STRATEGIC_FIRE,
                        button -> {

                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageStrategicFire(player.getUUID(), group.getId(), true));
                        });
                strategicFireButton.setTooltip(Tooltip.create(TOOLTIP_STRATEGIC_FIRE));
                addRenderableWidget(strategicFireButton);

                //HOLD FIRE/FIRE AT WILL
                RecruitsCommandButton holdFireButton = new RecruitsCommandButton(x, y - 25, TEXT_HOLD_FIRE,
                        button -> {
                            //Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player, state, groups, formation));
                        });
                holdFireButton.setTooltip(Tooltip.create(TOOLTIP_HOLD_FIRE));
                addRenderableWidget(holdFireButton);

                //SHIELDS
                RecruitsCommandButton shieldsButton = new RecruitsCommandButton(x, y + 25, TEXT_SHIELDS,
                        button -> {
                            //Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player, state, groups, formation));
                        });
                shieldsButton.setTooltip(Tooltip.create(TOOLTIP_SHIELDS));
                addRenderableWidget(shieldsButton);

                //CLEAR TARGETS
                RecruitsCommandButton clearTargetsButton = new RecruitsCommandButton(x, y + 50, TEXT_CLEAR_TARGET,
                        button -> {
                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), group.getId()));
                        });
                clearTargetsButton.setTooltip(Tooltip.create(TOOLTIP_CLEAR_TARGET));
                addRenderableWidget(clearTargetsButton);

                //PASSIVE
                RecruitsCommandButton passiveButton = new RecruitsCommandButton(x - 100, y - 25, TEXT_PASSIVE,
                        button -> {
                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group.getId()));
                        });
                passiveButton.setTooltip(Tooltip.create(TOOLTIP_PASSIVE));
                addRenderableWidget(passiveButton);

                //NEUTRAL
                RecruitsCommandButton neutralButton = new RecruitsCommandButton(x - 100, y + 25, TEXT_NEUTRAL,
                        button -> {
                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group.getId()));
                        });
                neutralButton.setTooltip(Tooltip.create(TOOLTIP_NEUTRAL));
                addRenderableWidget(neutralButton);

                //RAID
                RecruitsCommandButton raidButton = new RecruitsCommandButton(x + 100, y - 25, TEXT_RAID,
                        button -> {
                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group.getId()));
                        });
                raidButton.setTooltip(Tooltip.create(TOOLTIP_RAID));
                addRenderableWidget(raidButton);

                //AGGRESSIVE
                RecruitsCommandButton aggressiveButton = new RecruitsCommandButton(x + 100, y + 25, TEXT_AGGRESSIVE,
                        button -> {
                            if(!groups.isEmpty()) for(RecruitsGroup group : groups) Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group.getId()));
                        });
                aggressiveButton.setTooltip(Tooltip.create(TOOLTIP_AGGRESSIVE));
                addRenderableWidget(aggressiveButton);
            }

            case OTHER -> {
                //PROTECT
                RecruitsCommandButton protectButton = new RecruitsCommandButton(x, y - 25, TEXT_PROTECT,
                        button -> {
                            if (rayEntity != null && !groups.isEmpty()) {
                                for(RecruitsGroup group : groups) {
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageProtectEntity(player.getUUID(), rayEntity.getUUID(), group.getId()));
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 5, formation.getIndex(), group.getId()));
                                }
                            }
                        });
                protectButton.setTooltip(Tooltip.create(TOOLTIP_PROTECT));
                addRenderableWidget(protectButton);

                //MOUNT
                RecruitsCommandButton mountButton = new RecruitsCommandButton(x, y + 25, TEXT_MOUNT,
                        button -> {
                            //Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player, state, groups, formation));
                        });
                mountButton.setTooltip(Tooltip.create(TOOLTIP_MOUNT));
                addRenderableWidget(mountButton);

                //TEAM
                RecruitsCommandButton teamButton = new RecruitsCommandButton(x, y + 50, TEXT_TEAM,
                        button -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageTeamMainScreen(player));
                        });
                teamButton.setTooltip(Tooltip.create(TOOLTIP_TEAM));
                addRenderableWidget(teamButton);

                //BACK TO MOUNT
                RecruitsCommandButton backToMountButton = new RecruitsCommandButton(x + 100, y + 25, TEXT_BACK_TO_MOUNT,
                        button -> {
                            if (!groups.isEmpty()) {
                                for (RecruitsGroup group : groups) {
                                    if (!group.isDisabled()) {
                                        Main.SIMPLE_CHANNEL.sendToServer(new MessageBackToMountEntity(player.getUUID(), group.getId()));
                                    }
                                }
                            }
                        });
                backToMountButton.setTooltip(Tooltip.create(TOOLTIP_BACK_TO_MOUNT));
                addRenderableWidget(backToMountButton);

                //DISMOUNT
                RecruitsCommandButton dismountButton = new RecruitsCommandButton(x - 100, y + 25, TEXT_DISMOUNT,
                        button -> {
                            if (!groups.isEmpty()) {
                                for (RecruitsGroup group : groups) {
                                    if (!group.isDisabled()) {
                                        Main.SIMPLE_CHANNEL.sendToServer(new MessageDismount(player.getUUID(), group.getId()));
                                    }
                                }
                            }
                        });
                dismountButton.setTooltip(Tooltip.create(TOOLTIP_DISMOUNT));
                addRenderableWidget(dismountButton);

                //UPKEEP
                RecruitsCommandButton upkeepButton = new RecruitsCommandButton(x + 100, y, TEXT_UPKEEP,
                        button -> {
                            if (!groups.isEmpty()) {
                                for (RecruitsGroup group : groups) {
                                    if (!group.isDisabled() && rayEntity != null) {
                                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepEntity(player.getUUID(), rayEntity.getUUID(), group.getId()));
                                    } else if (!group.isDisabled() && rayBlockPos != null)
                                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepPos(player.getUUID(), group.getId(), this.rayBlockPos));
                                }
                            }
                        });
                upkeepButton.setTooltip(Tooltip.create(TOOLTIP_UPKEEP));
                addRenderableWidget(upkeepButton);

                //REST
                RecruitsCommandButton restButton = new RecruitsCommandButton(x - 100, y, TEXT_REST,
                        button -> {
                            //Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player, state, groups, formation));
                        });
                restButton.setTooltip(Tooltip.create(TOOLTIP_REST));
                addRenderableWidget(restButton);
            }
        }
    }

    private void sendMovementCommandToServer(int state) {
        if(state != 1){
            PlayerEvents.activeGroups = null;
            PlayerEvents.followFormation = false;
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

    public void sendMovementCommandInChat(int state){
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

            case 91 -> this.player.sendSystemMessage(TEXT_BACK_TO_MOUNT(group_string.toString()));
            case 92 -> this.player.sendSystemMessage(TEXT_UPKEEP(group_string.toString()));
            case 93 -> this.player.sendSystemMessage(TEXT_SHIELDS_OFF(group_string.toString()));
            case 94 -> this.player.sendSystemMessage(TEXT_STRATEGIC_FIRE_OFF(group_string.toString()));
            case 95 -> this.player.sendSystemMessage(TEXT_SHIELDS(group_string.toString()));
            case 96 -> this.player.sendSystemMessage(TEXT_STRATEGIC_FIRE(group_string.toString()));

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


    private static MutableComponent TEXT_HOLD_MY_POS(String group_string) {
        return Component.translatable("chat.recruits.command.holdMyPos", group_string);
    }

    private static MutableComponent TEXT_PROTECT(String group_string) {
        return Component.translatable("chat.recruits.command.protect", group_string);
    }

    private static MutableComponent TEXT_UPKEEP(String group_string) {
        return Component.translatable("chat.recruits.command.upkeep", group_string);
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

    private static MutableComponent TEXT_DISMOUNT(String group_string) {
        return Component.translatable("chat.recruits.command.dismount", group_string);
    }

    private static MutableComponent TEXT_MOUNT(String group_string) {
        return Component.translatable("chat.recruits.command.mount", group_string);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
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
        if(p_94688_ > 0) this.setSelection(selection.getBefore());
        else this.setSelection(selection.getNext());
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
    private enum Selection {
        COMBAT((byte) -1),
        MOVEMENT((byte) 0),
        OTHER((byte) 1);

        private final byte index;

        Selection(byte index) {
            this.index = index;
        }

        public byte getIndex() {
            return this.index;
        }

        public Selection getNext() {
            int length = values().length;
            byte newIndex = (byte) (this.index + 1);

            if (newIndex >= length -1) {
                return this;
            } else {
                return fromIndex(newIndex);
            }
        }

        public Selection getBefore() {
            byte newIndex = (byte) (this.index - 1);

            if (newIndex < -1) {
                return this;
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
    @OnlyIn(Dist.CLIENT)
    public enum Formation {
        NONE((byte) 0),
        LINE((byte) 1),
        SQUARE((byte) 2),
        TRIANGLE((byte) 3),
        VFORM((byte) 4),
        HCIRCLE((byte) 5),
        HSQUARE((byte) 6);

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
