package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.client.gui.team.TeamMainScreen;
import com.talhanation.recruits.network.*;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class OtherCategory implements ICommandCategory {

    private static final MutableComponent TOOLTIP_CLEAR_UPKEEP = Component.translatable("gui.recruits.command.tooltip.clear_upkeep");
    private static final MutableComponent TEXT_CLEAR_UPKEEP = Component.translatable("gui.recruits.command.text.clear_upkeep");
    private static final MutableComponent TEXT_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.text.backToMount");
    private static final MutableComponent TEXT_UPKEEP = Component.translatable("gui.recruits.command.text.upkeep");
    private static final MutableComponent TEXT_REST = Component.translatable("gui.recruits.command.text.rest");
    private static final MutableComponent TEXT_TEAM = Component.translatable("gui.recruits.command.text.team");
    private static final MutableComponent TOOLTIP_BACK_TO_MOUNT = Component.translatable("gui.recruits.command.tooltip.backToMount");
    private static final MutableComponent TOOLTIP_UPKEEP = Component.translatable("gui.recruits.command.tooltip.upkeep");
    private static final MutableComponent TOOLTIP_REST = Component.translatable("gui.recruits.command.tooltip.rest");
    private static final MutableComponent TOOLTIP_TEAM = Component.translatable("gui.recruits.command.tooltip.team");
    private static final MutableComponent TEXT_DISMOUNT = Component.translatable("gui.recruits.command.text.dismount");
    private static final MutableComponent TEXT_MOUNT = Component.translatable("gui.recruits.command.text.mount");
    private static final MutableComponent TOOLTIP_DISMOUNT = Component.translatable("gui.recruits.command.tooltip.dismount");
    private static final MutableComponent TOOLTIP_MOUNT = Component.translatable("gui.recruits.command.tooltip.mount");
    private static final MutableComponent TOOLTIP_PROTECT = Component.translatable("gui.recruits.command.tooltip.protect");
    private static final MutableComponent TEXT_PROTECT = Component.translatable("gui.recruits.command.text.protect");
    private static final MutableComponent TOOLTIP_OTHER = Component.translatable("gui.recruits.command.tooltip.other");

    @Override
    public Component getToolTipName() {
        return TOOLTIP_OTHER;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.CHEST);
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean isOneGroupActive = groups.stream().anyMatch(g -> !g.isDisabled());

        //PROTECT
        RecruitsCommandButton protectButton = new RecruitsCommandButton(x, y - 25, TEXT_PROTECT,
                button -> {
                    if (screen.rayEntity != null && !groups.isEmpty()) {
                        for(RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageProtectEntity(player.getUUID(), screen.rayEntity.getUUID(), group.getId()));
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageMovement(player.getUUID(), 5, CommandScreen.formation.getIndex(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(5);
                    }
                });
        protectButton.setTooltip(Tooltip.create(TOOLTIP_PROTECT));
        protectButton.active = isOneGroupActive && screen.rayEntity != null;
        screen.addRenderableWidget(protectButton);

        //MOUNT
        RecruitsCommandButton mountButton = new RecruitsCommandButton(x, y + 25, TEXT_MOUNT,
                button -> {
                    if (screen.rayEntity != null && !groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageMountEntity(player.getUUID(), screen.rayEntity.getUUID(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(99);
                    }
                });
        mountButton.setTooltip(Tooltip.create(TOOLTIP_MOUNT));
        mountButton.active = isOneGroupActive && screen.rayEntity != null;
        screen.addRenderableWidget(mountButton);

        //TEAM
        RecruitsCommandButton factionButton = new RecruitsCommandButton(x - 60, y + 50, TEXT_TEAM,
                button -> {
                    screen.getMinecraft().setScreen(new TeamMainScreen(player));
                });
        factionButton.setTooltip(Tooltip.create(TOOLTIP_TEAM));
        screen.addRenderableWidget(factionButton);

        //BACK TO MOUNT
        RecruitsCommandButton backToMountButton = new RecruitsCommandButton(x + 100, y + 25, TEXT_BACK_TO_MOUNT,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageBackToMountEntity(player.getUUID(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(91);
                    }
                });
        backToMountButton.setTooltip(Tooltip.create(TOOLTIP_BACK_TO_MOUNT));
        backToMountButton.active = isOneGroupActive;
        screen.addRenderableWidget(backToMountButton);

        //DISMOUNT
        RecruitsCommandButton dismountButton = new RecruitsCommandButton(x - 100, y + 25, TEXT_DISMOUNT,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageDismount(player.getUUID(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(98);
                    }
                });
        dismountButton.setTooltip(Tooltip.create(TOOLTIP_DISMOUNT));
        dismountButton.active = isOneGroupActive;
        screen.addRenderableWidget(dismountButton);

        //UPKEEP
        RecruitsCommandButton upkeepButton = new RecruitsCommandButton(x + 100, y, TEXT_UPKEEP,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled() && screen.rayEntity != null) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepEntity(player.getUUID(), screen.rayEntity.getUUID(), group.getId()));
                            } else if (!group.isDisabled() && screen.rayBlockPos != null)
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepPos(player.getUUID(), group.getId(), screen.rayBlockPos));
                        }
                        screen.sendCommandInChat(92);
                    }
                });
        upkeepButton.setTooltip(Tooltip.create(TOOLTIP_UPKEEP));
        upkeepButton.active = isOneGroupActive && (isUpkeepPosition(screen.rayBlockPos, player)|| isUpkeepEntity(screen.rayEntity));
        screen.addRenderableWidget(upkeepButton);

        //Clear Upkeep
        RecruitsCommandButton clearUpkeepButton = new RecruitsCommandButton(x + 60, y + 50, TEXT_CLEAR_UPKEEP,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageClearUpkeep(player.getUUID(), group.getId()));
                            }
                        }
                        screen.sendCommandInChat(93);
                    }
                });
        clearUpkeepButton.setTooltip(Tooltip.create(TOOLTIP_CLEAR_UPKEEP));
        clearUpkeepButton.active = isOneGroupActive;
        screen.addRenderableWidget(clearUpkeepButton);

        //REST
        RecruitsCommandButton restButton = new RecruitsCommandButton(x - 100, y, TEXT_REST,
                button -> {
                    if (!groups.isEmpty()) {
                        for (RecruitsGroup group : groups) {
                            if (!group.isDisabled()) {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageRest(player.getUUID(), group.getId(), true));
                            }
                        }
                        screen.sendCommandInChat(88);
                    }
                });
        restButton.setTooltip(Tooltip.create(TOOLTIP_REST));
        restButton.active = isOneGroupActive;
        screen.addRenderableWidget(restButton);
    }


    private boolean isUpkeepPosition(BlockPos rayBlockPos, Player player) {
        if(rayBlockPos == null) return false;

        BlockEntity entity = player.getCommandSenderWorld().getBlockEntity(rayBlockPos);
        BlockState blockState = player.getCommandSenderWorld().getBlockState(rayBlockPos);

        return entity instanceof Container || blockState.getBlock() instanceof ChestBlock;
    }

    private boolean isUpkeepEntity(Entity rayEntity) {
        if(rayEntity == null) return false;

        return rayEntity instanceof Container || rayEntity instanceof InventoryCarrier || rayEntity instanceof AbstractHorse;
    }
}

