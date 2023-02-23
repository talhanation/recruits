package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamListContainer;
import com.talhanation.recruits.network.MessageSendJoinRequestTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class TeamListScreen extends ScreenBase<TeamListContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_list_gui.png");
    Player player;
    public List<PlayerTeam> teams;
    private int leftPos;
    private int topPos;
    private ExtendedButton joinButton;
    private static final MutableComponent TEAMS_LIST = Component.translatable("gui.recruits.team_creation.teams_list");
    private static final MutableComponent NO_TEAMS = Component.translatable("gui.recruits.team_creation.no_teams");
    public TeamListScreen(TeamListContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
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


        for(int i = 0; i < teams.size(); i++) {
            if (i < 9) {
                PlayerTeam team = teams.get(i);
                String teamName = team.getName();
                joinButton = createJoinButton(team, teamName);
                if (player.getTeam() != null) {
                    joinButton.active = false;
                }
            }
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
        font.draw(matrixStack, TEAMS_LIST, 18, 11, fontColor);
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
                int y = 32 + (23 * i);

                font.draw(matrixStack, "" + name, x, y, teamFontColor);
                font.draw(matrixStack, "" + members + "/" + players, x + 80, y, teamFontColor);
            }
        }
        else
            font.draw(matrixStack, NO_TEAMS, 20, 26, fontColor);
    }

    public ExtendedButton createJoinButton(PlayerTeam team, String teamName) {
        return addRenderableWidget(new ExtendedButton(leftPos + 150, topPos + 25 + (23 * teams.indexOf(team)), 30, 15, Component.translatable("chat.recruits.team_creation.join"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSendJoinRequestTeam(player.getUUID(), teamName));
                    this.onClose();
                }
        ));
    }
}
