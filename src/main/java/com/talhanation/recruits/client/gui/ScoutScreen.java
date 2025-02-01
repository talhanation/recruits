package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.entities.ScoutEntity;
import com.talhanation.recruits.network.MessageScoutTask;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ScoutScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = new TranslatableComponent("gui.recruits.more_screen.title");
    private final Player player;
    private final ScoutEntity scout;
    private ScoutEntity.State task;
    private static final MutableComponent SCOUTING = new TranslatableComponent("gui.recruits.inv.text.scoutScoutTask");
    private static final MutableComponent TOOLTIP_SCOUTING = new TranslatableComponent("gui.recruits.inv.tooltip.scoutScoutTask");
    private ActivateableButton buttonScouting;

    public ScoutScreen(ScoutEntity scout, Player player) {
        super(TITLE, 195,160);
        this.player = player;
        this.scout = scout;
    }

    @Override
    protected void init() {
        super.init();
        this.task = ScoutEntity.State.fromIndex(scout.getTaskState());

        setButtons();
    }

    private void setButtons(){
        clearWidgets();

        buttonScouting = new ActivateableButton(guiLeft + 32, guiTop + ySize - 120 - 7, 130, 20, SCOUTING,
            btn -> {
                if(this.scout != null) {
                    if(task != ScoutEntity.State.SCOUTING){
                        task = ScoutEntity.State.SCOUTING;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageScoutTask(scout.getUUID(), 1));
                    }
                    else{
                        task = ScoutEntity.State.IDLE;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageScoutTask(scout.getUUID(), 0));
                    }

                    setButtons();
                }
            },
            (button, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_SCOUTING, i, i1);
            }
        );
        buttonScouting.active = task == ScoutEntity.State.SCOUTING;
        addRenderableWidget(buttonScouting);
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
        if(task != null){
            String text = "";
            if(task == ScoutEntity.State.IDLE) text = "No Active Task";
            else text = "Active Task: " + task.name();
            font.draw(poseStack, text, guiLeft + xSize / 2 - font.width(text) / 2, guiTop + 17, FONT_COLOR);
        }
    }
}
