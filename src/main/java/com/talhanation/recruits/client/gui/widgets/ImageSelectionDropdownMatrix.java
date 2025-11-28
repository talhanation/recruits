package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.group.EditOrAddGroupScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;

public class ImageSelectionDropdownMatrix extends AbstractWidget {

    protected static final int BG_FILL = FastColor.ARGB32.color(255, 80, 80, 80);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);

    private final List<ResourceLocation> options;
    private final Consumer<ResourceLocation> onSelect;
    private final EditOrAddGroupScreen parent;

    private ResourceLocation selectedOption;
    private boolean isOpen;

    private final int cellSize = 22;
    private final int columns = 4;
    private final int rows = 5;

    public ImageSelectionDropdownMatrix(EditOrAddGroupScreen parent, int x, int y, int width, int height, List<ResourceLocation> options, Consumer<ResourceLocation> onSelect) {
        super(x, y, width, height, Component.literal(""));
        this.parent = parent;
        this.options = options;
        this.onSelect = onSelect;
        this.selectedOption = parent.getSelectedImage();
    }


    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        gui.fill(getX(), getY(), getX() + width, getY() + height, BG_FILL_SELECTED);

        // Selected Image
        if (selectedOption != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, selectedOption);
            gui.blit(selectedOption, getX() + 3, getY() + 3, 0, 0, height - 6, height - 6, height - 6, height - 6);
        }

        // Dropdown
        if (isOpen) {
            int startX = getX() + width;
            int startY = getY();

            for (int i = 0; i < options.size(); i++) {
                int col = i % columns;
                int row = i / columns;

                int ox = startX + col * cellSize;
                int oy = startY + row * cellSize;

                ResourceLocation img = options.get(i);

                // Background
                if (mouseX >= ox && mouseX <= ox + cellSize && mouseY >= oy && mouseY <= oy + cellSize)
                    gui.fill(ox, oy, ox + cellSize, oy + cellSize, BG_FILL_HOVERED);
                else
                    gui.fill(ox, oy, ox + cellSize, oy + cellSize, BG_FILL);

                // Image
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, img);
                gui.blit(img, ox + 3, oy + 3, 0, 0, 16, 16, 16, 16);
            }
        }
    }


    public void onMouseClicked(double mouseX, double mouseY) {
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int col = i % columns;
                int row = i / columns;

                int optionX = this.getX() + this.width + col * cellSize;
                int optionY = this.getY() + row * cellSize;

                if (isMouseOverOption((int) mouseX, (int) mouseY, optionX, optionY)) {
                    selectOption(options.get(i));
                    isOpen = false;
                    return;
                }
            }
        }

        if (isMouseOverDisplay((int) mouseX, (int) mouseY)) {
            isOpen = !isOpen;
        } else {
            isOpen = false;
        }
    }

    public void onMouseMove(double mouseX, double mouseY) {
        if (isOpen) {
            boolean isOverDropdown = isMouseOverDropdown((int) mouseX, (int) mouseY);
            boolean isOverDisplay = isMouseOverDisplay((int) mouseX, (int) mouseY);

            if (!isOverDropdown && !isOverDisplay) {
                isOpen = false;
            }
        }
    }

    private boolean isMouseOverDisplay(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (!isOpen) return false;

        int dropdownStartX = this.getX() + this.width; // Dropdown rechts der Anzeige
        int dropdownStartY = this.getY();

        int dropdownEndX = dropdownStartX + columns * cellSize;
        int dropdownEndY = dropdownStartY + rows * cellSize;

        return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionX, int optionY) {
        return mouseX >= optionX && mouseX <= optionX + cellSize && mouseY >= optionY && mouseY <= optionY + cellSize;
    }


    private void selectOption(ResourceLocation option) {
        selectedOption = option;
        onSelect.accept(option);
        isOpen = false;
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
