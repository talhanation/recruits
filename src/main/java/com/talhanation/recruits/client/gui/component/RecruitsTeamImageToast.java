package com.talhanation.recruits.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecruitsTeamImageToast extends ImageToast {

    private final BannerRenderer bannerRenderer;
    public RecruitsTeamImageToast(ResourceLocation image, Component title, Component description, RecruitsTeam recruitsTeam) {
        super(image, title, description);
        this.bannerRenderer = new BannerRenderer(recruitsTeam);
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long deltaTime) {
        Visibility visibility = super.render(guiGraphics, toastComponent, deltaTime);

        int bannerX = 139;
        int bannerY = 3;
        if(image == null){
            bannerX = 3;
        }

        if (bannerRenderer != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            bannerRenderer.renderBanner(guiGraphics, bannerX, bannerY, width(), height(), 14);
        }

        return visibility;
    }
}
