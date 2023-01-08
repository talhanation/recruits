package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamListContainer;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.util.List;

public class TeamListScreen extends ScreenBase<TeamListContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_list_gui.png");
    Player player;
    public List<PlayerTeam> teams;
    private int leftPos;
    private int topPos;

    public TeamListScreen(TeamListContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 197;
        imageHeight = 250;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();

        this.teams = player.getScoreboard().getPlayerTeams().stream().toList();


        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;


        for(PlayerTeam team : teams){
            String teamName = team.getName();
            if(!player.getTeam().equals(team)) {
                addRenderableWidget(new Button(leftPos + 140, topPos + 30 + (25 * teams.indexOf(team)), 30, 20, new TranslatableComponent("chat.recruits.team_creation.join"),
                        button -> {
                            //if(joinRequests.contains(textField.getValue())) {
                            //Main.SIMPLE_CHANNEL.sendToServer(new MessageSendJoinRequest(player, teamName));
                            this.onClose();
                            //}
                        }
                ));
            }
            //Widget button pressed
        }
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        int teamFontColor = 4210752;

        List<PlayerTeam> playerTeams = teams.stream().toList();
        font.draw(matrixStack, "Teams in this world: ", 18, 11, fontColor);
        if(!playerTeams.isEmpty()) {
            for (int i = 0; i < playerTeams.size(); i++) {
                PlayerTeam playerTeam = playerTeams.get(i);
                String name = playerTeam.getName();
                List<String> allMembers = playerTeam.getPlayers().stream().toList();

                if (playerTeam.getColor().isColor()) {
                    teamFontColor = playerTeam.getColor().getColor();
                }

                int members = allMembers.size();
                int players = allMembers.stream().filter((str) -> str.chars().count() <= 16).toList().size();
                int x = 18;
                int y = 40 + (25 * i);

                font.draw(matrixStack, "" + name, x, y, teamFontColor);
                font.draw(matrixStack, "" + members + "/" + players, x + 60, y, fontColor);
            }
        }
        else
            font.draw(matrixStack, "There are no Teams in this world.", 18, 150, fontColor);
    }


}
