package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamAddPlayerContainer;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class TeamAddPlayerScreen extends ScreenBase<TeamAddPlayerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_add_player_gui.png");
    public static List<String> joinRequests;
    private EditBox textField;
    public Player player;
    private int leftPos;
    private int topPos;

    public TeamAddPlayerScreen(TeamAddPlayerContainer container, Inventory playerInventory, Component title) {
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

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        textField = new EditBox(font, leftPos + 18, topPos + 25, 140, 20, new TextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);

        addRenderableWidget(new Button(leftPos + 18, topPos + 55, 140, 20, new TranslatableComponent("chat.recruits.team_creation.addPlayer"),
                button -> {
                    //if(joinRequests.contains(textField.getValue())) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(player.getTeam().getName(), textField.getValue()));
                        this.onClose();
                    //}
                }
        ));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        font.draw(matrixStack, "Add Player to your Team:", 18  , 11, fontColor);

        font.draw(matrixStack, "Team Join Requests:", 18  , 82, fontColor);

        /*
        for(int i = 0; i < joinRequests.size(); i ++){
            String name = joinRequests.get(i);
            font.draw(matrixStack, "- " + name, 18, 92 + (12 * i), fontColor);
        }
         */
    }
}
