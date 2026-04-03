package com.talhanation.recruits.client.gui.diplomacy;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.network.MessageRemoveEmbargo;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class EmbargoScreen extends SelectPlayerScreen {

    private static final Component TITLE               = Component.translatable("gui.recruits.embargo.screen_title");
    private static final Component BUTTON_ADD          = Component.translatable("gui.recruits.embargo.button_add");
    private static final Component BUTTON_REMOVE       = Component.translatable("gui.recruits.embargo.button_remove");
    private static final Component TOOLTIP_LEADER_ONLY = Component.translatable("gui.recruits.embargo.tooltip_leader_only");

    public EmbargoScreen(Screen parent, Player player) {
        super(
                parent, player,
                TITLE,
                BUTTON_ADD,
                TOOLTIP_LEADER_ONLY,
                false,
                PlayersList.FilterType.EMBARGO,
                (playerInfo) -> {}
        );
    }

    @Override
    protected void init() {
        super.init();
        replaceActionButton();
        updateButtonState();
    }

    private void replaceActionButton() {
        removeWidget(actionButton);
        actionButton = addRenderableWidget(new ExtendedButton(
                actionButton.getX(), actionButton.getY(),
                actionButton.getWidth(), actionButton.getHeight(),
                BUTTON_ADD,
                button -> handleActionButton()
        ));
    }

    private void handleActionButton() {
        if (selected == null) {
            minecraft.setScreen(new EmbargoAddScreen(this, player));
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveEmbargo(selected.getUUID()));
            playerList.setFocused(null);
            playerList.updateEntryList();
            selected = null;
            init();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int z) {
        boolean flag = super.mouseClicked(x, y, z);
        updateButtonState();
        return flag;
    }

    private void updateButtonState() {
        boolean isLeader = ClientManager.ownFaction != null
                && ClientManager.ownFaction.getTeamLeaderUUID().equals(minecraft.player.getUUID());

        actionButton.setMessage(selected != null ? BUTTON_REMOVE : BUTTON_ADD);
        actionButton.active = isLeader;

        if (!isLeader) {
            actionButton.setTooltip(Tooltip.create(TOOLTIP_LEADER_ONLY));
        } else {
            actionButton.setTooltip(null);
        }
    }

    public Player getPlayer() {
        return player;
    }
}
