package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.network.*;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CombatCategory implements ICommandCategory {
    private static final MutableComponent TOOLTIP_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.tooltip.strategic_fire");
    private static final MutableComponent TOOLTIP_HOLD_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.tooltip.hold_strategic_fire");
    private static final MutableComponent TOOLTIP_SHIELDS_UP = Component.translatable("gui.recruits.command.tooltip.shields_up");
    private static final MutableComponent TOOLTIP_SHIELDS_DOWN = Component.translatable("gui.recruits.command.tooltip.shields_down");
    private static final MutableComponent TOOLTIP_PASSIVE = Component.translatable("gui.recruits.command.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = Component.translatable("gui.recruits.command.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = Component.translatable("gui.recruits.command.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = Component.translatable("gui.recruits.command.tooltip.raid");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.command.tooltip.clearTargets");
    private static final MutableComponent TOOLTIP_HOLD_FIRE = Component.translatable("gui.recruits.command.tooltip.hold_fire");
    private static final MutableComponent TOOLTIP_FIRE_AT_WILL = Component.translatable("gui.recruits.command.tooltip.fire_at_will");
    private static final MutableComponent TEXT_SHIELDS_UP = Component.translatable("gui.recruits.command.text.shields_up");
    private static final MutableComponent TEXT_SHIELDS_DOWN = Component.translatable("gui.recruits.command.text.shields_down");
    private static final MutableComponent TEXT_PASSIVE = Component.translatable("gui.recruits.command.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = Component.translatable("gui.recruits.command.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = Component.translatable("gui.recruits.command.text.aggressive");
    private static final MutableComponent TEXT_RAID = Component.translatable("gui.recruits.command.text.raid");
    private static final MutableComponent TEXT_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.text.strategic_fire");
    private static final MutableComponent TEXT_HOLD_STRATEGIC_FIRE = Component.translatable("gui.recruits.command.text.hold_strategic_fire");
    private static final MutableComponent TEXT_FIRE_AT_WILL = Component.translatable("gui.recruits.command.text.fire_at_will");
    private static final MutableComponent TEXT_HOLD_FIRE = Component.translatable("gui.recruits.command.text.hold_fire");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.command.text.clearTargets");
    private static final MutableComponent TOOLTIP_COMBAT = Component.translatable("gui.recruits.command.tooltip.combat");

    @Override
    public Component getToolTipName() {
        return TOOLTIP_COMBAT;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.IRON_SWORD);
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean isOneGroupActive = groups.stream().anyMatch(g -> !g.isDisabled());

        //STRATEGIC FIRE
        RecruitsCommandButton strategicFireButton = new RecruitsCommandButton(x, y - 50, TEXT_STRATEGIC_FIRE,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageStrategicFire(player.getUUID(), group.getId(), true));
                            }
                        }
                        screen.sendCommandInChat(72);
                    }

                });
        strategicFireButton.setTooltip(Tooltip.create(TOOLTIP_STRATEGIC_FIRE));
        strategicFireButton.active = isOneGroupActive && screen.rayBlockPos != null;
        screen.addRenderableWidget(strategicFireButton);

        //HOLD STRATEGIC FIRE
        RecruitsCommandButton holdStrategicFireButton = new RecruitsCommandButton(x, y - 25, TEXT_HOLD_STRATEGIC_FIRE,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageStrategicFire(player.getUUID(), group.getId(), false));
                            }
                        }
                        screen.sendCommandInChat(73);
                    }

                });
        holdStrategicFireButton.setTooltip(Tooltip.create(TOOLTIP_HOLD_STRATEGIC_FIRE));
        holdStrategicFireButton.active = isOneGroupActive;
        screen.addRenderableWidget(holdStrategicFireButton);

        //FIRE AT WILL
        RecruitsCommandButton fireAtWillButton = new RecruitsCommandButton(x + 100, y - 38, TEXT_FIRE_AT_WILL,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageRangedFire(player.getUUID(), group.getId(), true));
                            }
                        }
                        screen.sendCommandInChat(70);
                    }
                });
        fireAtWillButton.setTooltip(Tooltip.create(TOOLTIP_FIRE_AT_WILL));
        fireAtWillButton.active = isOneGroupActive;
        screen.addRenderableWidget(fireAtWillButton);

        //HOLD FIRE
        RecruitsCommandButton holdFireButton = new RecruitsCommandButton(x + 100, y - 13, TEXT_HOLD_FIRE,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageStrategicFire(player.getUUID(), group.getId(), false));
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageRangedFire(player.getUUID(), group.getId(), false));
                            }
                        }
                        screen.sendCommandInChat(71);
                    }
                });
        holdFireButton.setTooltip(Tooltip.create(TOOLTIP_HOLD_FIRE));
        holdFireButton.active = isOneGroupActive;
        screen.addRenderableWidget(holdFireButton);

        //SHIELDS UP
        RecruitsCommandButton shieldsUpButton = new RecruitsCommandButton(x + 100, y + 13, TEXT_SHIELDS_UP,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageShields(player.getUUID(), group.getId(), true));
                            }
                        }
                        screen.sendCommandInChat(74);
                    }
                });
        shieldsUpButton.setTooltip(Tooltip.create(TOOLTIP_SHIELDS_UP));
        shieldsUpButton.active = isOneGroupActive;
        screen.addRenderableWidget(shieldsUpButton);

        //SHIELDS DOWN
        RecruitsCommandButton shieldsDownButton = new RecruitsCommandButton(x + 100, y + 38, TEXT_SHIELDS_DOWN,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageShields(player.getUUID(), group.getId(), false));
                            }
                        }
                        screen.sendCommandInChat(75);
                    }
                });
        shieldsDownButton.setTooltip(Tooltip.create(TOOLTIP_SHIELDS_DOWN));
        shieldsDownButton.active = isOneGroupActive;
        screen.addRenderableWidget(shieldsDownButton);

        //FORGET TARGETS
        RecruitsCommandButton clearTargetsButton = new RecruitsCommandButton(x, y + 50, TEXT_CLEAR_TARGET,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(9);
                    }
                });
        clearTargetsButton.setTooltip(Tooltip.create(TOOLTIP_CLEAR_TARGET));
        clearTargetsButton.active = isOneGroupActive;
        screen.addRenderableWidget(clearTargetsButton);

        //PASSIVE
        RecruitsCommandButton passiveButton = new RecruitsCommandButton(x - 100, y - 38, TEXT_PASSIVE,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group.getId()));
                            }
                        }
                        screen.sendCommandInChat(13);
                    }
                });
        passiveButton.setTooltip(Tooltip.create(TOOLTIP_PASSIVE));
        passiveButton.active = isOneGroupActive;
        screen.addRenderableWidget(passiveButton);

        //NEUTRAL
        RecruitsCommandButton neutralButton = new RecruitsCommandButton(x - 100, y - 13 , TEXT_NEUTRAL,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group.getId()));
                            }
                        }
                        screen.sendCommandInChat(10);
                    }
                });
        neutralButton.setTooltip(Tooltip.create(TOOLTIP_NEUTRAL));
        neutralButton.active = isOneGroupActive;
        screen.addRenderableWidget(neutralButton);

        //RAID
        RecruitsCommandButton raidButton = new RecruitsCommandButton(x - 100, y + 38, TEXT_RAID,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group.getId()));
                            }
                        }
                        screen.sendCommandInChat(12);
                    }
                });
        raidButton.setTooltip(Tooltip.create(TOOLTIP_RAID));
        raidButton.active = isOneGroupActive;
        screen.addRenderableWidget(raidButton);

        //AGGRESSIVE
        RecruitsCommandButton aggressiveButton = new RecruitsCommandButton(x - 100, y + 13, TEXT_AGGRESSIVE,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group.getId()));
                            }
                        }
                        screen.sendCommandInChat(11);
                    }
                });
        aggressiveButton.setTooltip(Tooltip.create(TOOLTIP_AGGRESSIVE));
        aggressiveButton.active = isOneGroupActive;
        screen.addRenderableWidget(aggressiveButton);
    }
}

