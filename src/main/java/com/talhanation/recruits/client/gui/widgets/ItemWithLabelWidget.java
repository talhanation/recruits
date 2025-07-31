package com.talhanation.recruits.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemWithLabelWidget extends AbstractWidget {

    private final ItemStack itemStack;
    private final Component label;
    private final boolean drawDecoration;
    private final boolean drawBackground;

    public ItemWithLabelWidget(int x, int y, int width, int height, ItemStack itemStack, Component label, boolean drawDecoration, boolean drawBackground) {
        super(x, y, width, height, label);
        this.itemStack = itemStack;
        this.label = label;
        this.drawDecoration = drawDecoration;
        this.drawBackground = drawBackground;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // Hintergrund zeichnen (halbtransparent grau)
        if (drawBackground) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xAA000000);
        }

        // Item rendern
        int itemX = getX() + 2;
        int itemY = getY() + (height - 16) / 2; // zentrieren
        guiGraphics.renderItem(itemStack, itemX, itemY);
        if (drawDecoration) {
            guiGraphics.renderItemDecorations(mc.font, itemStack, itemX, itemY);
        }

        // Text rendern
        int textX = itemX + 20;
        int textY = getY() + (this.height - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, label, textX, textY, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {

    }
}