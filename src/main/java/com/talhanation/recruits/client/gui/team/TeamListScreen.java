package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.inventory.TeamListContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

public class TeamListScreen extends ScreenBase<TeamListContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");
    Player player;
    BannerBlockEntity bannerBlockEntity;
    public static List<PlayerTeam> teams;
    private int leftPos;
    private int topPos;
    protected int imageWidth = 176;
    protected int imageHeight = 166;

    public TeamListScreen(TeamListContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;


        Main.LOGGER.debug("---------TeamListScreen--------");
        Main.LOGGER.debug("");


        for (int i = 0; i < teams.size(); i++){

        }


        Main.LOGGER.debug("-------------------------------");
    }

    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

}
