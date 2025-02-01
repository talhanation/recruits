package com.talhanation.recruits.client.gui.widgets;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class ListScreenBase extends RecruitsScreenBase {

    private Runnable postRender;

    public ListScreenBase(Component title, int xSize, int ySize) {
        super(title, xSize, ySize);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        super.render(poseStack, mouseX, mouseY, delta);
        if (postRender != null) {
            postRender.run();
            postRender = null;
        }
    }

    public void postRender(Runnable postRender) {
        this.postRender = postRender;
    }

}

