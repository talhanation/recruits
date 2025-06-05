package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ICommandCategory {
    Component getToolTipName();
    ItemStack getIcon();
    void createButtons(CommandScreen screen, int centerX, int centerY, List<RecruitsGroup> groups, Player player);
}
