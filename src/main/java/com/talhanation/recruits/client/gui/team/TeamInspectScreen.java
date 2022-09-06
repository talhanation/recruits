package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.network.MessageLeaveTeam;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public class TeamInspectScreen extends Screen {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");
    private static final TranslatableComponent LEAVE_TEAM = new TranslatableComponent("gui.recruits.teamcreation.leave_team");
    Player player;
    Team team;
    private int leftPos;
    private int topPos;
    protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected TeamInspectScreen(Player player, Team team) {
        super(new TranslatableComponent("gui.recruits.teamInspectTitle"));
        this.player = player;
        this.team = team;
    }


    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        int players = TeamEvents.getTeamPlayerMembersCount(team);
        int npcs = TeamEvents.getTeamNPCMembersCount(team);
        int members = players + npcs;

        //leave team up right
        addRenderableWidget(new Button(leftPos + 77 + 55, topPos + 4, 40, 12, LEAVE_TEAM, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
            this.onClose();
        }));

        //inspect banner

        //team members
        //team count player members
        //team count npc members

        //send message to team members

    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
