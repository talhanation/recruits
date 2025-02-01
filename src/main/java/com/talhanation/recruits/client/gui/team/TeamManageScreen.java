package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.network.MessageAddPlayerToTeam;
import com.talhanation.recruits.network.MessageRemoveFromTeam;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class TeamManageScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = new TranslatableComponent("gui.recruits.team.manage");
    private static final MutableComponent BACK = new TranslatableComponent("gui.recruits.button.back");
    private static final MutableComponent ADD_PLAYER = new TranslatableComponent("gui.recruits.team.addPlayer");
    private static final MutableComponent REMOVE_PLAYER = new TranslatableComponent("gui.recruits.team.removePlayer");
    private static final MutableComponent TOOLTIP_WIP = new TranslatableComponent("gui.recruits.wip");
    private static final MutableComponent TOOLTIP_ADD_PLAYER = new TranslatableComponent("gui.recruits.team.tooltip.add");
    private static final MutableComponent TOOLTIP_REMOVE_PLAYER = new TranslatableComponent("gui.recruits.team.tooltip.remove");
    private static final MutableComponent PLAYER_PROMOTION = new TranslatableComponent("gui.recruits.team.playerPromotion");
    private static final MutableComponent UNIT_MANAGEMENT = new TranslatableComponent("gui.recruits.team.unitManagement");
    private final Player player;
    private final RecruitsTeam recruitsTeam;
    private final Screen parent;

    public TeamManageScreen(Screen parent, Player player, RecruitsTeam recruitsTeam) {
        super(TITLE, 195,160);
        this.parent = parent;
        this.player = player;
        this.recruitsTeam = recruitsTeam;
    }

    @Override
    protected void init() {
        super.init();

        setButtons();
    }

    private void setButtons(){
        clearWidgets();

        Button addPlayer = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 120 - 7, 130, 20, ADD_PLAYER,
            btn -> {
                minecraft.setScreen(new SelectPlayerScreen(this, player, TOOLTIP_ADD_PLAYER,  ADD_PLAYER, new TextComponent(""), false, PlayersList.FilterType.TEAM_JOIN_REQUEST,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(recruitsTeam.getStringID(), playerInfo.getName()));
                        }
                ));
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_ADD_PLAYER, i, i1);
            }
        ));

        Button removePlayer = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 98 - 7, 130, 20, REMOVE_PLAYER,
            btn -> {
                minecraft.setScreen(new SelectPlayerScreen(this, player, TOOLTIP_REMOVE_PLAYER,  REMOVE_PLAYER, new TextComponent(""), false, PlayersList.FilterType.SAME_TEAM,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveFromTeam(playerInfo.getName()));
                        }
                ));
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_REMOVE_PLAYER, i, i1);
            }
        ));

        Button unitManagement = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 76 - 7, 130, 20, UNIT_MANAGEMENT,
            btn -> {

            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_WIP, i, i1);
            }
        ));
        unitManagement.active = false;

        Button playerPromotion = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 54 - 7, 130, 20, PLAYER_PROMOTION,
            btn -> {

            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_WIP, i, i1);
            }
        ));
        playerPromotion.active = false;

        Button back = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 32 - 7, 130, 20, BACK,
            btn -> {
                minecraft.setScreen(parent);
            }
        ));
    }


    @Override
    public void renderBackground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        font.draw(poseStack, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR);

    }

}
