package com.talhanation.recruits.client.gui.widgets;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public abstract class ListScreenListBase<T extends ListScreenEntryBase<T>> extends ContainerObjectSelectionList<T> {

    public ListScreenListBase(int width, int height, int x, int y, int size) {
        super(Minecraft.getInstance(), width, y - x, x, size);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        super.renderWidget(guiGraphics, x, y, partialTicks);
    }

    // 1.20.1: setRenderBackground(false) - the screens draw their own background texture
    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    // 1.20.1: setRenderTopAndBottom(false)
    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

}
