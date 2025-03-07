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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class TeamManageScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.team.manage");
    private static final MutableComponent BACK = Component.translatable("gui.recruits.button.back");
    private static final MutableComponent ADD_PLAYER = Component.translatable("gui.recruits.team.addPlayer");
    private static final MutableComponent REMOVE_PLAYER = Component.translatable("gui.recruits.team.removePlayer");
    private static final MutableComponent TOOLTIP_WIP = Component.translatable("gui.recruits.wip");
    private static final MutableComponent TOOLTIP_ADD_PLAYER = Component.translatable("gui.recruits.team.tooltip.add");
    private static final MutableComponent TOOLTIP_REMOVE_PLAYER = Component.translatable("gui.recruits.team.tooltip.remove");
    private static final MutableComponent PLAYER_PROMOTION = Component.translatable("gui.recruits.team.playerPromotion");
    private static final MutableComponent UNIT_MANAGEMENT = Component.translatable("gui.recruits.team.unitManagement");
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

        Button addPlayer = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 120 - 7, 130, 20, ADD_PLAYER,
            btn -> {
                minecraft.setScreen(new SelectPlayerScreen(this, player, TOOLTIP_ADD_PLAYER,  ADD_PLAYER, Component.literal(""), false, PlayersList.FilterType.TEAM_JOIN_REQUEST,
                        (playerInfo) -> {
                            recruitsTeam.removeJoinRequest(playerInfo.getName());
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAddPlayerToTeam(recruitsTeam.getStringID(), playerInfo.getName()));
                        }
                ));
            }
        ));
        addPlayer.setTooltip(Tooltip.create(TOOLTIP_ADD_PLAYER));
        Button removePlayer = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 98 - 7, 130, 20, REMOVE_PLAYER,
            btn -> {
                minecraft.setScreen(new SelectPlayerScreen(this, player, TOOLTIP_REMOVE_PLAYER,  REMOVE_PLAYER, Component.literal(""), false, PlayersList.FilterType.SAME_TEAM,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageRemoveFromTeam(playerInfo.getName()));
                        }
                ));
            }
        ));
        removePlayer.setTooltip(Tooltip.create(TOOLTIP_REMOVE_PLAYER));
        Button unitManagement = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 76 - 7, 130, 20, UNIT_MANAGEMENT,
            btn -> {

            }
        ));
        unitManagement.active = false;
        unitManagement.setTooltip(Tooltip.create(TOOLTIP_WIP));

        Button playerPromotion = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 54 - 7, 130, 20, PLAYER_PROMOTION,
            btn -> {

            }
        ));
        playerPromotion.active = false;
        playerPromotion.setTooltip(Tooltip.create(TOOLTIP_WIP));

        Button back = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 32 - 7, 130, 20, BACK,
            btn -> {
                minecraft.setScreen(parent);
            }
        ));
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
