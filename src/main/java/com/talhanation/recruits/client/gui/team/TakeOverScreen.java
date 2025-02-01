package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.DisbandScreen;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageAssignToTeamMate;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class TakeOverScreen extends RecruitsScreenBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.team.manage");
    private static final MutableComponent MORE = Component.translatable("gui.recruits.inv.more");
    private static final MutableComponent OPEN_INVENTORY = Component.translatable("gui.recruits.inv.openInventory");
    private static final MutableComponent TAKE_OWNERSHIP = Component.translatable("gui.recruits.inv.takeOwnership");
    private static final MutableComponent TOOLTIP_TAKE_OWNERSHIP = Component.translatable("gui.recruits.inv.tooltip.takeOwnership");
    private static final MutableComponent TOOLTIP_OPEN_INVENTORY = Component.translatable("gui.recruits.inv.tooltip.openInventory");
    private static final MutableComponent TOOLTIP_MORE = Component.translatable("gui.recruits.inv.tooltip.more");
    private final Player player;
    private final AbstractRecruitEntity recruit;

    public TakeOverScreen(AbstractRecruitEntity recruit, Player player) {
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

        Button takeOwnerShip = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 120 - 7, 130, 20, TAKE_OWNERSHIP,
            btn -> {
                 Main.SIMPLE_CHANNEL.sendToServer(new MessageAssignToTeamMate(this.recruit.getUUID(), this.player.getUUID()));
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_TAKE_OWNERSHIP, i, i1);
            }
        ));

        Button openInventory = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 98 - 7, 130, 20, OPEN_INVENTORY,
            btn -> {
                this.recruit.openGUI(this.player);
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_OPEN_INVENTORY, i, i1);
            }
        ));

        Button more = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 76 - 7, 130, 20, MORE,
            btn -> {
                minecraft.setScreen(new DisbandScreen(this, this.recruit, this.player));
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_MORE, i, i1);
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
