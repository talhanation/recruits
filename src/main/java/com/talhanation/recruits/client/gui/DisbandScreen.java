package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.group.RecruitsGroupListScreen;
import com.talhanation.recruits.client.gui.group.RenameRecruitScreen;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAssignRecruitToPlayer;
import com.talhanation.recruits.network.MessageDisband;
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

public class DisbandScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.more_screen.title");
    private Player player;
    private AbstractRecruitEntity recruit;
    private static final MutableComponent DISBAND = Component.translatable("gui.recruits.inv.text.disband");
    private static final MutableComponent TOOLTIP_DISBAND = Component.translatable("gui.recruits.inv.tooltip.disband");
    public static final MutableComponent TOOLTIP_KEEP_TEAM = Component.translatable("gui.recruits.inv.tooltip.keepTeam");
    public static final MutableComponent TOOLTIP_ASSIGN_GROUP_TO_PLAYER = Component.translatable("gui.recruits.inv.tooltip.assignGroupToPlayer");
    private static final MutableComponent ASSIGN_TO_PLAYER = Component.translatable("gui.recruits.team.assignNewOwner");
    private static final MutableComponent GROUP_SETTINGS = Component.translatable("gui.recruits.groups.settings");
    private static final MutableComponent RENAME = Component.translatable("gui.recruits.inv.rename");
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

        Button buttonRename = new ExtendedButton(guiLeft + 32, guiTop + 25, 130, 20, RENAME,
                btn -> {
                    if(recruit != null) {
                        minecraft.setScreen(new RenameRecruitScreen(this, recruit));
                    }
                }
        );
        addRenderableWidget(buttonRename);

        Button buttonDisband = new ExtendedButton(guiLeft + 32, guiTop + 45, 130, 20, DISBAND,
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
        );
        buttonDisband.setTooltip(Tooltip.create(TOOLTIP_DISBAND));
        addRenderableWidget(buttonDisband);

        Button giveToPlayer = new ExtendedButton(guiLeft + 32, guiTop + 65, 130, 20, ASSIGN_TO_PLAYER,
            btn -> {
                if(recruit != null) {
                    minecraft.setScreen(new SelectPlayerScreen(this, player, ASSIGN_TO_PLAYER, ASSIGN_TO_PLAYER, TOOLTIP_ASSIGN_GROUP_TO_PLAYER, false, PlayersList.FilterType.NONE,
                        (playerInfo) -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignRecruitToPlayer(this.recruit.getUUID(), playerInfo.getUUID()));
                            onClose();
                        })
                    );
                }
            }
        );
        addRenderableWidget(giveToPlayer);

        Button buttonGroupSettings = new ExtendedButton(guiLeft + 32, guiTop + 105, 130, 20, GROUP_SETTINGS,
                btn -> {
                    minecraft.setScreen(new RecruitsGroupListScreen(player));
                }
        );
        addRenderableWidget(buttonGroupSettings);
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
