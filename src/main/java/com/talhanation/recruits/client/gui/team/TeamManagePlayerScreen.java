package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamManagePlayerContainer;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import com.talhanation.recruits.network.MessageRemoveFromTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
@OnlyIn(Dist.CLIENT)
public class TeamManagePlayerScreen extends ScreenBase<TeamManagePlayerContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_manage_player_gui.png");
    public static List<String> joinRequests;
    public List<? extends Player> playersList;
    private List<String> onlinePlayerJoinRequests;
    private EditBox textField;
    private final Player player;
    private int leftPos;
    private int topPos;

    private static final MutableComponent TOOLTIP_ADD = Component.translatable("gui.recruits.team.tooltip.add");

    public TeamManagePlayerScreen(TeamManagePlayerContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
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
        playersList = player.getCommandSenderWorld().players().stream().toList();
        onlinePlayerJoinRequests = new ArrayList<>();

        for(Player onlinePlayer : playersList) {
            String name = onlinePlayer.getName().getString();
            for (String requestName : joinRequests) {
                if (name.equals(requestName))
                    onlinePlayerJoinRequests.add(requestName);
            }
        }


        textField = new EditBox(font, leftPos + 18, topPos + 25, 140, 20, Component.literal(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);
        textField.setCanLoseFocus(false);

        addRenderableWidget(textField);
        setInitialFocus(textField);

        addRenderableWidget(new ExtendedButton(leftPos + 18, topPos + 55, 140, 20, Component.translatable("chat.recruits.team_creation.removePlayer"),
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
    }

    protected void containerTick() {
        super.containerTick();
        if(textField != null) textField.tick();
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        guiGraphics.drawString(font, Component.translatable("chat.recruits.team_creation.removePlayerTitle"), 18, 11, fontColor, false);
        guiGraphics.drawString(font, Component.translatable("chat.recruits.team_creation.addPlayerTitle"), 18, 82, fontColor, false);



        for(int i = 0; i < onlinePlayerJoinRequests.size(); i ++){
            String requestName = onlinePlayerJoinRequests.get(i);
            int x = 18;
            int y = 98 + (23 * i);
            guiGraphics.drawString(font, "- " + requestName, x,y, fontColor, false);
        }
    }

    public Button createAddButton(String playerNameToAdd, String teamName) {
        Button button = new ExtendedButton(leftPos + 110, topPos + 93 + (23 * onlinePlayerJoinRequests.indexOf(playerNameToAdd)), 30, 15, Component.translatable(  "gui.recruits.team_creation.add_player_Button"),
                onPress -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(teamName, playerNameToAdd));
                    this.onClose();
                });
        button.setTooltip(Tooltip.create(TOOLTIP_ADD));
        return addRenderableWidget(button);

    }
}
