package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAssignGroupToTeamMate;
import com.talhanation.recruits.network.MessageAssignToTeamMate;
import com.talhanation.recruits.network.MessageDisband;
import com.talhanation.recruits.network.MessageDisbandGroup;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class DisbandScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.more_screen.title");
    private Player player;
    private AbstractRecruitEntity recruit;
    private static final MutableComponent DISBAND = Component.translatable("gui.recruits.inv.text.disband");
    private static final MutableComponent DISBAND_GROUP = Component.translatable("gui.recruits.inv.text.disbandGroup");
    private static final MutableComponent TOOLTIP_DISBAND = Component.translatable("gui.recruits.inv.tooltip.disband");
    private static final MutableComponent TOOLTIP_KEEP_TEAM = Component.translatable("gui.recruits.inv.tooltip.keepTeam");
    private static final MutableComponent TOOLTIP_DISBAND_GROUP = Component.translatable("gui.recruits.inv.tooltip.disbandGroup");
    private static final MutableComponent TOOLTIP_ASSIGN_TO_MATE = Component.translatable("gui.recruits.inv.tooltip.assignToTeamMate");
    private static final MutableComponent TOOLTIP_ASSIGN_GROUP_TO_MATE = Component.translatable("gui.recruits.inv.tooltip.assignGroupToTeamMate");
    private static final MutableComponent TEAM_MATE = Component.translatable("gui.recruits.team.assignNewOwner");
    private static final MutableComponent TEAM_MATE_GROUP = Component.translatable("gui.recruits.team.assignGroupNewOwner");

    public DisbandScreen(Screen parent, AbstractRecruitEntity recruit, Player player) {
        super(TITLE, 195,160);
        this.player = player;
        this.recruit = recruit;
    }

    @Override
    protected void init() {
        super.init();

        setButtons();
    }

    private void setButtons(){
        clearWidgets();

        Button buttonDisband = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 120 - 7, 130, 20, DISBAND,
                btn -> {
                    if(this.recruit != null) {
                        if(this.recruit.getTeam() != null) {
                            minecraft.setScreen(new ConfirmScreen(DISBAND, TOOLTIP_KEEP_TEAM,
                                    () -> Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(this.recruit.getUUID(), true)),
                                    () -> Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(this.recruit.getUUID(), false)),
                                    () -> minecraft.setScreen(DisbandScreen.this)
                            ));
                        }
                        else
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(this.recruit.getUUID(), false));
                    }
                }
        ));
        buttonDisband.setTooltip(Tooltip.create(TOOLTIP_DISBAND));

        Button giveToTeamMate = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 98 - 7, 130, 20, TEAM_MATE,

            btn -> {
                if(recruit != null) {
                    minecraft.setScreen(new SelectPlayerScreen(this, player, TEAM_MATE, TEAM_MATE, TOOLTIP_ASSIGN_GROUP_TO_MATE, false, PlayersList.FilterType.SAME_TEAM,
                            (playerInfo) -> {
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignToTeamMate(this.recruit.getUUID(), playerInfo.getUUID()));
                                onClose();
                            } )
                    );
                }
            }
        ));
        giveToTeamMate.active = player.getTeam() != null;


        Button buttonDisbandGroup = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 76 - 7, 130, 20, DISBAND_GROUP,
            btn -> {
                if(this.recruit != null) {
                    if(recruit.getTeam() != null){
                        minecraft.setScreen(new ConfirmScreen(DISBAND_GROUP, TOOLTIP_KEEP_TEAM,
                                () ->  Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.recruit.getUUID(), true)),
                                () ->  Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.recruit.getUUID(), false)),
                                () ->  minecraft.setScreen(DisbandScreen.this)
                        ));
                    }
                    else
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageDisbandGroup(this.player.getUUID(), this.recruit.getUUID(), false));
                }
            }
        ));
        buttonDisbandGroup.setTooltip(Tooltip.create(TOOLTIP_DISBAND_GROUP));
        addRenderableWidget(buttonDisbandGroup);

        Button buttonAssignGroup = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 54 - 7, 130, 20, TEAM_MATE_GROUP,
            btn -> {
                if(recruit != null) {
                    minecraft.setScreen(new SelectPlayerScreen(this, player, TEAM_MATE_GROUP, TEAM_MATE_GROUP, TOOLTIP_ASSIGN_GROUP_TO_MATE, false, PlayersList.FilterType.SAME_TEAM,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignGroupToTeamMate(this.player.getUUID(), playerInfo.getUUID(), this.recruit.getUUID()));
                            onClose();
                        } )
                    );
                }
            }
        ));
        buttonAssignGroup.active = player.getTeam() != null;
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR, false);

    }

}
