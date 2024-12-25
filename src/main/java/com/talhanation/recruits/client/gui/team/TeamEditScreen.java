package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.ColorSelectButton;
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

import java.awt.*;

public class TeamEditScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = new TranslatableComponent("gui.recruits.team.edit");
    private static final MutableComponent BACK = new TranslatableComponent("gui.recruits.button.back");
    private final Player player;
    private final RecruitsTeam recruitsTeam;
    private final Screen parent;

    public TeamEditScreen(Screen parent, Player player, RecruitsTeam recruitsTeam) {
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

        ColorSelectButton colorRed = new ColorSelectButton(Color.RED, guiLeft + 32, guiTop + ySize, 20, 20, new TextComponent(""),
                btn -> {
                    minecraft.setScreen(parent);
                }
        );
        addRenderableWidget(colorRed);






        Button back = addRenderableWidget(new Button(guiLeft + 32, guiTop + ySize - 32 - 7, 130, 20, BACK,
                btn -> {
                    minecraft.setScreen(parent);
                }
        ));
        addRenderableWidget(back);
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
