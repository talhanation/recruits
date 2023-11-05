package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.DisbandContainer;
import com.talhanation.recruits.network.MessageAssignGroupToTeamMate;
import com.talhanation.recruits.network.MessageAssignToTeamMate;
import com.talhanation.recruits.network.MessageDisband;
import com.talhanation.recruits.network.MessageDisbandGroup;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class DisbandScreen extends ScreenBase<DisbandContainer> {


    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_main_gui.png");
    private Player player;
    private UUID recruit;
    private int leftPos;
    private int topPos;
    private static final MutableComponent DISBAND = Component.translatable("gui.recruits.inv.text.disband");
    private static final MutableComponent DISBAND_GROUP = Component.translatable("gui.recruits.inv.text.disbandGroup");
    private static final MutableComponent TOOLTIP_DISBAND = Component.translatable("gui.recruits.inv.tooltip.disband");
    private static final MutableComponent TOOLTIP_KEEP_TEAM = Component.translatable("gui.recruits.inv.tooltip.keepTeam");
    private static final MutableComponent TOOLTIP_DISBAND_GROUP = Component.translatable("gui.recruits.inv.tooltip.disbandGroup");
    private static final MutableComponent TOOLTIP_ASSIGN_TO_MATE = Component.translatable("gui.recruits.inv.tooltip.assignToTeamMate");
    private static final MutableComponent TOOLTIP_ASSIGN_GROUP_TO_MATE = Component.translatable("gui.recruits.inv.tooltip.assignGroupToTeamMate");
    private static final MutableComponent TEAM_MATE = Component.translatable("gui.recruits.team.assignNewOwner");
    private static final MutableComponent TEAM_MATE_GROUP = Component.translatable("gui.recruits.team.assignGroupNewOwner");
    private boolean keepTeam;
    public DisbandScreen(DisbandContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.imageWidth = 250;
        this.imageHeight = 83;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        keepTeam = false;

        setButtons();
    }

    private void setButtons(){
        clearWidgets();

        Button buttonKeepTeam = addRenderableWidget(new Button(leftPos + 230, topPos + 15, 30, 20, Component.literal(String.valueOf(keepTeam)),
            btn -> {
                keepTeam = !keepTeam;
                setButtons();
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_KEEP_TEAM, i, i1);
            }
        ));

        Button giveToTeamMate = addRenderableWidget(new Button(leftPos + 20, topPos + 15, 100, 20, TEAM_MATE,
            btn -> {
                if(recruit != null) {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignToTeamMate(this.recruit));
                    onClose();
                }
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_ASSIGN_TO_MATE, i, i1);
            }
        ));

        Button buttonDisband = addRenderableWidget(new Button(leftPos + 130, topPos + 15, 100, 20, DISBAND,
            btn -> {
                if(this.recruit != null) {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(this.recruit, this.keepTeam));
                    onClose();
                }
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_DISBAND, i, i1);
            }
        ));

        Button buttonDisbandGroup = addRenderableWidget(new Button(leftPos + 130, topPos + 40, 100, 20, DISBAND_GROUP,
            btn -> {
                if(this.recruit != null) {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.recruit, this.keepTeam));
                    onClose();
                }
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_DISBAND_GROUP, i, i1);
            }
        ));

        Button buttonAssignGroup = addRenderableWidget(new Button(leftPos + 20, topPos + 40, 100, 20, TEAM_MATE_GROUP,
            btn -> {
                if(recruit != null) {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignGroupToTeamMate(this.player.getUUID(), this.recruit));
                    onClose();
                }
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_ASSIGN_GROUP_TO_MATE, i, i1);
            }
        ));
    }


    protected void render(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, RESOURCE_LOCATION);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

}
