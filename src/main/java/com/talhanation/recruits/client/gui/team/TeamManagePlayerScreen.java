package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamManagePlayerContainer;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import com.talhanation.recruits.network.MessageRemoveFromTeam;
import com.talhanation.recruits.network.MessageSendJoinRequestTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.Arrays;
import java.util.List;

public class TeamManagePlayerScreen extends ScreenBase<TeamManagePlayerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_manage_player_gui.png");
    public static List<String> joinRequests;
    public static List<? extends Player> playersList;
    private EditBox textField;
    public Player player;
    private int leftPos;
    private int topPos;
    private ExtendedButton addButton;

    public TeamManagePlayerScreen(TeamManagePlayerContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        imageWidth = 176;
        imageHeight = 225;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;


        //remove players that are not online from the list joinRequests
        playersList = player.getLevel().players().stream().toList();
        for(Player onlinePlayer : playersList) {
            String name = onlinePlayer.getName().getString();
            for (int i = 0; i < joinRequests.size(); i++) {
                String requestName = joinRequests.get(i);
                if(!name.contains(requestName))
                    joinRequests.remove(i);
            }

        }


        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        textField = new EditBox(font, leftPos + 18, topPos + 25, 140, 20, new TextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);

        addRenderableWidget(new ExtendedButton(leftPos + 18, topPos + 55, 140, 20, new TranslatableComponent("chat.recruits.team_creation.removePlayer"),
                button -> {
                    if(!textField.getValue().isEmpty()) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveFromTeam(textField.getValue()));
                        this.onClose();
                    }
                    init();
                }
        ));

        for (int i = 0; i < joinRequests.size(); i++) {
            if (i < 9) {
                String requestName = joinRequests.get(i);
                addButton = createAddButton(requestName, player.getTeam().getName());
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        font.draw(matrixStack, new TranslatableComponent("chat.recruits.team_creation.removePlayerTitle"), 18  , 11, fontColor);

        font.draw(matrixStack,  new TranslatableComponent("chat.recruits.team_creation.addPlayerTitle"), 18  , 82, fontColor);

        List<String> showingPlayers;

        for(int i = 0; i < joinRequests.size(); i ++){
            for(Player onlinePlayer : playersList) {
                String name = onlinePlayer.getName().getString();
                String requestName = joinRequests.get(i);
                int x = 18;
                int y = 100 + (23 * i);
                if(name.equals(requestName))
                    font.draw(matrixStack, "- " + requestName, x, y, fontColor);
            }
        }
    }

    public ExtendedButton createAddButton(String playerName, String teamName) {
        return addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 93 + (23 * joinRequests.indexOf(playerName)), 30, 15, new TranslatableComponent(  "gui.recruits.team_creation.add_player_Button"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(teamName, playerName));
                    this.onClose();
                }
        ));
    }
}
