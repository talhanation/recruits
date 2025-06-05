package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.group.RecruitsFormationButton;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.network.*;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class MovementCategory implements ICommandCategory {

    private static final MutableComponent TEXT_MOVE = Component.translatable("gui.recruits.command.text.move");
    private static final MutableComponent TEXT_MOVE_HOLD = Component.translatable("gui.recruits.command.text.move_hold");
    private static final MutableComponent TEXT_FORWARD = Component.translatable("gui.recruits.command.text.forward");
    private static final MutableComponent TEXT_BACKWARD = Component.translatable("gui.recruits.command.text.backward");
    private static final MutableComponent TEXT_FOLLOW = Component.translatable("gui.recruits.command.text.follow");
    private static final MutableComponent TEXT_WANDER = Component.translatable("gui.recruits.command.text.wander");
    private static final MutableComponent TEXT_HOLD_POS = Component.translatable("gui.recruits.command.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = Component.translatable("gui.recruits.command.text.backToPos");
    private static final MutableComponent TEXT_FORMATION_LINEUP = Component.translatable("gui.recruits.command.text.formation_lineup");
    private static final MutableComponent TEXT_FORMATION_SQUARE = Component.translatable("gui.recruits.command.text.formation_square");
    private static final MutableComponent TEXT_FORMATION_TRIANGLE = Component.translatable("gui.recruits.command.text.formation_triangle");
    private static final MutableComponent TEXT_FORMATION_HOLLOW_SQUARE = Component.translatable("gui.recruits.command.text.formation_hollow_square");
    private static final MutableComponent TEXT_FORMATION_HOLLOW_CIRCLE = Component.translatable("gui.recruits.command.text.formation_hollow_circle");
    private static final MutableComponent TEXT_FORMATION_V = Component.translatable("gui.recruits.command.text.formation_v");
    private static final MutableComponent TEXT_FORMATION_CIRCLE = Component.translatable("gui.recruits.command.text.formation_circle");
    private static final MutableComponent TEXT_FORMATION_MOVEMENT = Component.translatable("gui.recruits.command.text.formation_movement");
    private static final MutableComponent TEXT_FORMATION_NONE = Component.translatable("gui.recruits.command.text.formation_none");
    private static final MutableComponent TEXT_HOLD_MY_POS = Component.translatable("gui.recruits.command.text.holdMyPos");
    private static final MutableComponent TOOLTIP_MOVE = Component.translatable("gui.recruits.command.tooltip.move_hold");
    private static final MutableComponent TOOLTIP_FORWARD = Component.translatable("gui.recruits.command.tooltip.forward");
    private static final MutableComponent TOOLTIP_FORMATION = Component.translatable("gui.recruits.command.tooltip.formation");
    private static final MutableComponent TOOLTIP_BACKWARD = Component.translatable("gui.recruits.command.tooltip.backward");
    private static final MutableComponent TOOLTIP_FOLLOW = Component.translatable("gui.recruits.command.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = Component.translatable("gui.recruits.command.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = Component.translatable("gui.recruits.command.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = Component.translatable("gui.recruits.command.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = Component.translatable("gui.recruits.command.tooltip.backToPos");
    private static final MutableComponent TOOLTIP_MOVEMENT = Component.translatable("gui.recruits.command.tooltip.movement");

    @Override
    public Component getToolTipName() {
        return TOOLTIP_MOVEMENT;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.LEATHER_BOOTS);
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean isOneGroupActive = groups.stream().anyMatch(g -> !g.isDisabled());
        RecruitsCommandButton moveButton = new RecruitsCommandButton(x, y - 50, TEXT_MOVE,
                button -> {
                    screen.sendMovementCommandToServer(6);
                    screen.sendCommandInChat(6);
                });
        moveButton.setTooltip(Tooltip.create(TOOLTIP_MOVE));
        moveButton.active = isOneGroupActive && screen.rayBlockPos != null;
        screen.addRenderableWidget(moveButton);

        //FORWARD
        RecruitsCommandButton forwardButton = new RecruitsCommandButton(x - 60, y - 25, TEXT_FORWARD,
                button -> {
                    screen.sendMovementCommandToServer(7);
                    screen.sendCommandInChat(7);
                });
        forwardButton.setTooltip(Tooltip.create(TOOLTIP_FORWARD));
        forwardButton.active = isOneGroupActive;
        screen.addRenderableWidget(forwardButton);

        //FOLLOW
        RecruitsCommandButton followButton = new RecruitsCommandButton(x + 60, y - 25, TEXT_FOLLOW,
                button -> {
                    if (CommandScreen.formation.getIndex() != 0) {
                        List<RecruitsGroup> activeGroups = new ArrayList<>();
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) activeGroups.add(group);
                        }

                        int[] array = new int[activeGroups.size()];
                        for (int i = 0; i < activeGroups.size(); i++) {
                            RecruitsGroup group = activeGroups.get(i);
                            if (!group.isDisabled()) {
                                array[i] = group.getId();
                            }
                        }
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageSaveFormationFollowMovement(player.getUUID(), array, CommandScreen.formation.getIndex()));
                    } else {
                        screen.sendMovementCommandToServer(1);
                    }
                    screen.sendCommandInChat(1);
                });
        followButton.setTooltip(Tooltip.create(TOOLTIP_FOLLOW));
        followButton.active = isOneGroupActive;
        screen.addRenderableWidget(followButton);


        //WANDER FREELY
        RecruitsCommandButton wanderButton = new RecruitsCommandButton(x + 120, y, TEXT_WANDER,
                button -> {
                    screen.sendMovementCommandToServer(0);
                    screen.sendCommandInChat(0);

                });
        wanderButton.setTooltip(Tooltip.create(TOOLTIP_WANDER));
        wanderButton.active = isOneGroupActive;
        screen.addRenderableWidget(wanderButton);

        //BACK TO POS
        RecruitsCommandButton backToPosButton = new RecruitsCommandButton(x - 120, y, TEXT_BACK_TO_POS,
                button -> {
                    screen.sendMovementCommandToServer(3);
                    screen.sendCommandInChat(3);

                });
        backToPosButton.setTooltip(Tooltip.create(TOOLTIP_BACK_TO_POS));
        backToPosButton.active = isOneGroupActive;
        screen.addRenderableWidget(backToPosButton);

        //HOLDPOS
        RecruitsCommandButton holdPosButton = new RecruitsCommandButton(x + 60, y + 25, TEXT_HOLD_POS,
                button -> {
                    screen.sendMovementCommandToServer(2);
                    screen.sendCommandInChat(2);
                });
        holdPosButton.setTooltip(Tooltip.create(TOOLTIP_HOLD_POS));
        holdPosButton.active = isOneGroupActive;
        screen.addRenderableWidget(holdPosButton);

        //BACKWARD
        RecruitsCommandButton backwardButton = new RecruitsCommandButton(x - 60, y + 25, TEXT_BACKWARD,
                button -> {
                    screen.sendMovementCommandToServer(8);
                    screen.sendCommandInChat(8);
                });
        backwardButton.setTooltip(Tooltip.create(TOOLTIP_BACKWARD));
        backwardButton.active = isOneGroupActive;
        screen.addRenderableWidget(backwardButton);

        //NONE
        RecruitsFormationButton noneFormationButton = new RecruitsFormationButton(CommandScreen.Formation.NONE, x, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.NONE);
                });
        noneFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_NONE));
        screen.addRenderableWidget(noneFormationButton);

        //LINE UP
        RecruitsFormationButton lineUpFormationButton = new RecruitsFormationButton(CommandScreen.Formation.LINE, x - 21, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.LINE);
                });
        lineUpFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_LINEUP));
        screen.addRenderableWidget(lineUpFormationButton);

        //SQUARE
        RecruitsFormationButton squareFormationButton = new RecruitsFormationButton(CommandScreen.Formation.SQUARE, x + 21, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.SQUARE);
                });
        squareFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_SQUARE));
        screen.addRenderableWidget(squareFormationButton);

        //TRIANGLE
        RecruitsFormationButton triangleFormationButton = new RecruitsFormationButton(CommandScreen.Formation.TRIANGLE, x - 42, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.TRIANGLE);
                });
        triangleFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_TRIANGLE));
        screen.addRenderableWidget(triangleFormationButton);

        //V_FORM
        RecruitsFormationButton VFormFormationButton = new RecruitsFormationButton(CommandScreen.Formation.VFORM, x + 42, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.VFORM);
                });
        VFormFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_V));
        screen.addRenderableWidget(VFormFormationButton);

        //H CIRCLE
        RecruitsFormationButton hcircleFormationButton = new RecruitsFormationButton(CommandScreen.Formation.HCIRCLE, x - 63, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.HCIRCLE);
                });
        hcircleFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_HOLLOW_CIRCLE));
        screen.addRenderableWidget(hcircleFormationButton);

        //H SQAURE
        RecruitsFormationButton hSquareFormationButton = new RecruitsFormationButton(CommandScreen.Formation.HSQUARE, x + 63, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.HSQUARE);
                });
        hSquareFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_HOLLOW_SQUARE));
        screen.addRenderableWidget(hSquareFormationButton);

        //CIRCLE
        RecruitsFormationButton circleFormationButton = new RecruitsFormationButton(CommandScreen.Formation.CIRCLE, x - 84, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.CIRCLE);
                });
        circleFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_CIRCLE));
        screen.addRenderableWidget(circleFormationButton);

        //MOVEMENT
        RecruitsFormationButton movementFormationButton = new RecruitsFormationButton(CommandScreen.Formation.MOVEMENT, x + 84, y + 50,
                button -> {
                    screen.setFormation(CommandScreen.Formation.MOVEMENT);
                });
        movementFormationButton.setTooltip(Tooltip.create(TEXT_FORMATION_MOVEMENT));
        screen.addRenderableWidget(movementFormationButton);

        noneFormationButton.active = CommandScreen.formation == CommandScreen.Formation.NONE;
        lineUpFormationButton.active = CommandScreen.formation == CommandScreen.Formation.LINE;
        squareFormationButton.active = CommandScreen.formation == CommandScreen.Formation.SQUARE;
        hSquareFormationButton.active = CommandScreen.formation == CommandScreen.Formation.HSQUARE;
        triangleFormationButton.active = CommandScreen.formation == CommandScreen.Formation.TRIANGLE;
        VFormFormationButton.active = CommandScreen.formation == CommandScreen.Formation.VFORM;
        hcircleFormationButton.active = CommandScreen.formation == CommandScreen.Formation.HCIRCLE;
        circleFormationButton.active = CommandScreen.formation == CommandScreen.Formation.CIRCLE;
        movementFormationButton.active = CommandScreen.formation == CommandScreen.Formation.MOVEMENT;


    }
}

