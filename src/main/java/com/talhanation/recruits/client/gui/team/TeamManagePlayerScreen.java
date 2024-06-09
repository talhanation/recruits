package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamManagePlayerContainer;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import com.talhanation.recruits.network.MessageRemoveFromTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TeamManagePlayerScreen extends ScreenBase<TeamManagePlayerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_manage_player_gui.png");
    public static List<String> joinRequests;
    public List<? extends Player> playersList;
    private List<String> onlinePlayerJoinRequests;
    private EditBox textField;
    private final Player player;
    private int leftPos;
    private int topPos;

    private static final MutableComponent TOOLTIP_ADD = new TranslatableComponent("gui.recruits.team.tooltip.add");

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



        //add players to a list that are online and are in request list
        playersList = player.getLevel().players().stream().toList();
        onlinePlayerJoinRequests = new ArrayList<>();

        for(Player onlinePlayer : playersList) {
            String name = onlinePlayer.getName().getString();
            for (String requestName : joinRequests) {
                if (name.equals(requestName))
                    onlinePlayerJoinRequests.add(requestName);
            }
        }

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        textField = new EditBox(font, leftPos + 18, topPos + 25, 140, 20, new TextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);
        textField.setCanLoseFocus(false);

        addRenderableWidget(textField);
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

        for (int i = 0; i < onlinePlayerJoinRequests.size(); i++) {
            if (i < 9) {
                String requestName = onlinePlayerJoinRequests.get(i);
                Button addButton = createAddButton(requestName, player.getTeam().getName());
            }
        }
    }

    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    protected void containerTick() {
        super.containerTick();
        textField.tick();
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        setFocused(textField);

        return textField.keyPressed(key, a, b) || textField.canConsumeInput() || super.keyPressed(key, a, b);
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textField.isFocused()) {
            this.textField.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }
    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        font.draw(matrixStack, new TranslatableComponent("chat.recruits.team_creation.removePlayerTitle"), 18  , 11, fontColor);
        font.draw(matrixStack,  new TranslatableComponent("chat.recruits.team_creation.addPlayerTitle"), 18  , 82, fontColor);

        for(int i = 0; i < onlinePlayerJoinRequests.size(); i ++){
            String requestName = onlinePlayerJoinRequests.get(i);
            int x = 18;
            int y = 98 + (23 * i);
            font.draw(matrixStack, "- " + requestName, x, y, fontColor);
        }
    }

    public Button createAddButton(String playerNameToAdd, String teamName) {
        return addRenderableWidget(new Button(leftPos + 110, topPos + 93 + (23 * onlinePlayerJoinRequests.indexOf(playerNameToAdd)), 30, 15, new TranslatableComponent(  "gui.recruits.team_creation.add_player_Button"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(teamName, playerNameToAdd));
                this.onClose();
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_ADD, i, i1);
            }
        ));

    }
}
