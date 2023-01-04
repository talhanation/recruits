package com.talhanation.recruits.client.gui.team;

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

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");
    public static List<String> joinRequests;
    private EditBox textField;
    public Player player;
    private int leftPos;
    private int topPos;
    protected int imageWidth = 176;
    protected int imageHeight = 166;

    public TeamAddPlayerScreen(TeamAddPlayerContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        imageWidth = 176;
        imageHeight = 218;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        textField = new EditBox(font, leftPos + 30, topPos + 30, 116, 16, new TextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);

        addRenderableWidget(new Button(leftPos + 30, topPos + 50, 80, 20, new TranslatableComponent("chat.recruits.team_creation.addPlayer"),
                button -> {
                    if(joinRequests.contains(textField.getValue())) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(player.getTeam().getName(), textField.getValue()));
                        this.onClose();
                    }
                }
        ));
    }

}
