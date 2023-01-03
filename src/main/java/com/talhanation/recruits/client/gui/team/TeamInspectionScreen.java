package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.inventory.TeamInspectionContainer;
import com.talhanation.recruits.network.MessageLeaveTeam;
import com.talhanation.recruits.network.MessageOpenTeamAddPlayerScreen;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.UUID;

public class TeamInspectionScreen extends ScreenBase<TeamInspectionContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_inspect_gui.png");
    private static final TranslatableComponent LEAVE_TEAM = new TranslatableComponent("gui.recruits.teamcreation.leave_team");
    private static final TranslatableComponent EDIT_TEAM = new TranslatableComponent("gui.recruits.teamcreation.edit_team");
    private static final TranslatableComponent ADD_PLAYER_TEAM = new TranslatableComponent("gui.recruits.teamcreation.add_player");
    public static UUID leaderUUID;
    private final Player player;
    private final Team team;
    private ModelPart flag;
    private int leftPos;
    private int topPos;
    protected int imageWidth = 176;
    protected int imageHeight = 250;
    private int players;
    private int npcs;
    private int members;

    private List<Pair<BannerPattern, DyeColor>> resultBannerPatterns;
    public static ItemStack bannerItem;
    public static String leader;

    public TeamInspectionScreen(TeamInspectionContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        player = container.getPlayerEntity();
        team = player.getTeam();
    }

    @Override
    protected void init() {
        super.init();
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
        this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem)bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(bannerItem));

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.players = TeamEvents.getTeamPlayerMembersCount(team);
        this.npcs = TeamEvents.getTeamNPCMembersCount(team);
        this.members = players + npcs;
        boolean isTeamLeader = player.getUUID().equals(leaderUUID);


        if(isTeamLeader){

            //Edit Team
            addRenderableWidget(new Button(leftPos + 8, topPos + 220, 50, 18, EDIT_TEAM, button -> {
                //ain.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
                this.onClose();
            }));

            //Add Player
            addRenderableWidget(new Button(leftPos + 118, topPos + 220, 50, 18, ADD_PLAYER_TEAM, button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamAddPlayerScreen(player));
                this.onClose();
            }));
        }

        //leave team
        addRenderableWidget(new Button(leftPos + 63, topPos + 220, 50, 18, LEAVE_TEAM, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
            this.onClose();
        }));

        //inspect banner

        //team members
        //team count player members
        //team count npc members

        //send message to team members
        Main.LOGGER.debug("---------TeamInspectScreen--------");
        Main.LOGGER.debug("team: " + team.getName());
        Main.LOGGER.debug("leader: " + leader);
        Main.LOGGER.debug("total members: " + members);
        Main.LOGGER.debug("players: " + players);
        Main.LOGGER.debug("npcs: " + npcs);
        Main.LOGGER.debug("bannerItem: " + bannerItem);
        Main.LOGGER.debug("resultBannerPatterns: " + resultBannerPatterns);
        Main.LOGGER.debug("--------------------------------");
    }

    protected void render(PoseStack matrixStack, int partialTicks, int mouseX, int mouseY) {
        super.render(matrixStack, partialTicks, mouseX, mouseY);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        int k = (int)(41.0F);//                                                 (this.displayPatterns ? 0 : 12)
        this.blit(matrixStack, leftPos + 65, topPos + 22 + k, 232 + 0, 0, 12, 15);
        Lighting.setupForFlatItems();

        MultiBufferSource.BufferSource multibuffersource$buffersource = this.minecraft.renderBuffers().bufferSource();
        matrixStack.pushPose();
        matrixStack.translate((double)(leftPos), (double)(topPos), 0.0D);
        matrixStack.scale(32.0F, -32.0F, 1.0F);
        matrixStack.translate(1D, 1D, 1D);
        float f = 0.6666667F;
        matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.flag.xRot = 0.0F;
        this.flag.y = -32.0F;
        BannerRenderer.renderPatterns(matrixStack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, this.resultBannerPatterns);
        matrixStack.popPose();
        multibuffersource$buffersource.endBatch();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        font.draw(matrixStack, "" + team.getName(), 65, 10, fontColor);

        font.draw(matrixStack, "Leader:", 15 , 100, fontColor);
        font.draw(matrixStack, "" + leader, 15 + 50 ,  100, fontColor);

        font.draw(matrixStack, "Members:",  15  ,  115, fontColor);
        font.draw(matrixStack, "" + members, 15  + 50 , 115, fontColor);

        font.draw(matrixStack, "Players:", 15  , 130, fontColor);
        font.draw(matrixStack, "" + players, 15 + 50 , 130, fontColor);

        font.draw(matrixStack, "NPCs:", 15  , 145, fontColor);
        font.draw(matrixStack, "" + npcs, 15 + 50 , 145, fontColor);
    }
}
