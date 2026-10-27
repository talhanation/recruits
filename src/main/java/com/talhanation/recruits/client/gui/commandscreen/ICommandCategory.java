package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ICommandCategory {
    Component getToolTipName();
    ItemStack getIcon();
    void createButtons(CommandScreen screen, int centerX, int centerY, List<RecruitsGroup> groups, Player player);

    /** Categories can hide themselves, e.g. the Governor tab outside of an own claim with a Governor. */
    default boolean isAvailable(CommandScreen screen, Player player) {
        return true;
    }

    /** Called once when the command screen opens. */
    default void onOpen(CommandScreen screen, Player player) {
    }

    /** Called every client tick while this category is selected. */
    default void tick(CommandScreen screen, Player player) {
    }

    /** Mouse wheel over the screen; return true if the category used it (then the category is not switched). */
    default boolean mouseScrolled(CommandScreen screen, double mouseX, double mouseY, double delta) {
        return false;
    }
}
