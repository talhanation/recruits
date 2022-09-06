package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.scores.Team;

public class TeamMainScreen extends Screen {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");
    Player player;
    BannerBlockEntity bannerBlockEntity;
    private int leftPos;
    private int topPos;
    protected int imageWidth = 176;
    protected int imageHeight = 166;

    private static final Component CREATE_TEAM = new TranslatableComponent("gui.recruits.teamcreation.create_team");
    private static final Component INSPECT_TEAM = new TranslatableComponent("gui.recruits.teamcreation.inspect_team");
    private static final Component EDIT_TEAM = new TranslatableComponent("gui.recruits.teamcreation.edit_team");

    protected TeamMainScreen() {
        super(new TranslatableComponent("gui.recruits.mainTeamTitle"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        boolean isInTeam = TeamEvents.isPlayerInTeam(player);
        boolean isTeamLeader = TeamEvents.isPlayerTeamLeader(player, player.getTeam());

        String create;
        if(!isTeamLeader)
        create = isInTeam ? INSPECT_TEAM.getString() : CREATE_TEAM.getString();
        else
            create = EDIT_TEAM.getString();

        String list =

        addRenderableWidget(new Button(leftPos + 30, topPos + 30, 80, 20, new TextComponent(create), btn -> {
            if (isInTeam && !isTeamLeader) {
                minecraft.setScreen(new TeamInspectScreen(player, player.getTeam()));
            } else if (isTeamLeader) {
                TeamEvents.openTeamEditorScreen(player);
            }
            else{
                TeamEvents.openTeamCreationScreen(player);
            }
        }));

        addRenderableWidget(new Button(leftPos + 30, topPos + 50, 80, 20, new TextComponent(create), btn -> {
            minecraft.setScreen(new ScreenTeamList(this));
        }));
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);


        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderBanner.renderBanner(matrixStack, partialTicks, buffer, )InInventory();
        BannerRenderer.renderPatterns(matrixStack, buffer, pac);
    }

}
