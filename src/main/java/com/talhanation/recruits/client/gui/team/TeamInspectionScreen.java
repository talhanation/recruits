package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.talhanation.recruits.Main;
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
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;
import java.util.UUID;

public class TeamInspectionScreen extends ScreenBase<TeamInspectionContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_inspect_gui.png");
    private static final MutableComponent LEAVE_TEAM = Component.translatable("gui.recruits.team_creation.leave_team");
    private static final MutableComponent DELETE_TEAM = Component.translatable("gui.recruits.team_creation.delete_team");
    private static final MutableComponent EDIT_TEAM = Component.translatable("gui.recruits.team_creation.edit_team");
    private static final MutableComponent MANAGE_TEAM = Component.translatable("gui.recruits.team_creation.add_player");

    private static final MutableComponent TOOLTIP_COMING_SOON = Component.translatable("gui.recruits.team_creation.coming_soon");
    public static UUID leaderUUID;
    private final Player player;
    private final Team team;
    private ModelPart flag;
    private int leftPos;
    private int topPos;
    public static int players;
    public static int npcs;
    private int members;
    private List<Pair<Holder<BannerPattern>, DyeColor>> resultBannerPatterns;
    public static ItemStack bannerItem;
    public static String leader;

    public static List<String> playerMembers;
    private Button editButton;

    public TeamInspectionScreen(TeamInspectionContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        player = container.getPlayerEntity();
        team = player.getTeam();
        imageWidth = 220;
        imageHeight = 250;
    }

    protected void init() {
        super.init();
        playerMembers = player.getTeam().getPlayers().stream().filter((name) -> name.length() <= 16).toList();
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
        if(bannerItem != null) this.resultBannerPatterns = BannerBlockEntity.createPatterns(((BannerItem)bannerItem.getItem()).getColor(), BannerBlockEntity.getItemPatterns(bannerItem));

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.members = players + npcs;
        boolean isTeamLeader = player.getUUID().equals(leaderUUID);

        if(isTeamLeader){
            editButton = createEditButton();
            editButton.active = false;

            //Manage
            addRenderableWidget(new ExtendedButton(leftPos + 155, topPos + 218, 50, 18, MANAGE_TEAM, button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamAddPlayerScreen(player));
            }));
        }

        //leave team
        addRenderableWidget(new ExtendedButton(leftPos + 85, topPos + 218, 50, 18, (isTeamLeader ? DELETE_TEAM : LEAVE_TEAM), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageLeaveTeam());
            this.onClose();
        }));
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
        //LoomScreen
        if(bannerItem != null) {
            int k = (int) (41.0F);//                                                 (this.displayPatterns ? 0 : 12)
            this.blit(matrixStack, leftPos + 119, topPos + 13 + k, 232 + 0, 0, 12, 15);
            Lighting.setupForFlatItems();

            MultiBufferSource.BufferSource multibuffersource$buffersource = this.minecraft.renderBuffers().bufferSource();
            matrixStack.pushPose();
            matrixStack.translate((double) (leftPos) + 110, (double) (topPos) + 94, 0.0D);
            matrixStack.scale(54.0F, -54.0F, 1.0F);
            float f = 0.6666667F;
            matrixStack.scale(f, -f, -f);
            this.flag.xRot = 0.0F;
            this.flag.y = -32.0F;
            BannerRenderer.renderPatterns(matrixStack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true, this.resultBannerPatterns);
            matrixStack.popPose();
            multibuffersource$buffersource.endBatch();
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        //Info
        int fontColor = 4210752;
        int fontColorLeader;
        if(player.getTeam() != null && player.getTeam().getColor().getColor() != null) fontColorLeader = player.getTeam().getColor().getColor();
        else fontColorLeader = fontColor;


        font.draw(matrixStack, "" + team.getName(), 90, 10, fontColor);

        font.draw(matrixStack, "Leader:", 18 , 25, fontColor);
        font.draw(matrixStack, "" + leader, 18 ,  25 + 15, fontColorLeader);

        font.draw(matrixStack, "Players:", 18  , 116, fontColor);


        int xOffset = 18;
        int yOffset = 135;
        for (int i = 0; i < playerMembers.size(); i++) {
            String name = playerMembers.get(i);
            int xPosition = xOffset;
            int yPosition = yOffset + (12 * i);

            if (i >= 7) {
                xPosition += 100;
                yPosition -= 12;
            }

            font.draw(matrixStack, "- " + name, xPosition, yPosition, fontColor);
        }

        font.draw(matrixStack, "Members:",  135  ,  25, fontColor);
        font.draw(matrixStack, "" + members, 135 + 50 , 25, fontColor);

        font.draw(matrixStack, "Players:",  135, 40, fontColor);
        font.draw(matrixStack, "" + players, 135 + 50, 40, fontColor);

        font.draw(matrixStack, "NPCs:", 135  , 55, fontColor);
        font.draw(matrixStack, "" + npcs, 135 + 50 , 55, fontColor);
    }

    private Button createEditButton(){
        //Edit Team
        return addRenderableWidget(new ExtendedButton(leftPos + 15, topPos + 218, 50, 18, EDIT_TEAM,
            button -> {
            }
        ));

    }
}
