package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import net.minecraft.client.gui.GuiGraphics;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class TeamMainScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID,"textures/gui/gui_small.png");
    private static final MutableComponent CREATE_TEAM = Component.translatable("gui.recruits.team_creation.create_team");
    private static final MutableComponent INSPECT_TEAM = Component.translatable("gui.recruits.team_creation.inspect_team");
    private static final MutableComponent TEAMS_LIST = Component.translatable("gui.recruits.team_creation.teams_list");
    private final Player player;
    public TeamMainScreen(Player player){
        super(Component.literal("TeamMainScreen"),246,84);
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        boolean isInTeam = TeamEvents.isPlayerInATeam(player);

        MutableComponent mutableComponent = isInTeam ? INSPECT_TEAM : CREATE_TEAM;
        addRenderableWidget(new ExtendedButton(guiLeft + 20, guiTop + 29, 100, 20, mutableComponent, btn -> {
            if (isInTeam && player.getTeam() != null) {
                minecraft.setScreen(new TeamInspectionScreen(this, player));
            }
            else {
                TeamEvents.openTeamEditScreen(player);
            }
        }));

        addRenderableWidget(new ExtendedButton(guiLeft + 130, guiTop + 29, 100, 20, TEAMS_LIST, btn -> {
            minecraft.setScreen(new RecruitsTeamListScreen(this));
        }));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

}
