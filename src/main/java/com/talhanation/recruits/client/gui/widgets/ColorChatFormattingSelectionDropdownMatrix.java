package com.talhanation.recruits.client.gui.widgets;

import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;

public class ColorChatFormattingSelectionDropdownMatrix extends AbstractWidget {
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    private final List<ChatFormatting> options;
    private final Consumer<ChatFormatting> onSelect;
    private final TeamEditScreen parent;
    private ChatFormatting selectedOption;
    private boolean isOpen;
    private final int cellSize = 20;
    private final int colorSize = 16;
    private final int columns;
    private final int rows;
    private final String name;

    public ColorChatFormattingSelectionDropdownMatrix(TeamEditScreen parent, int x, int y, int width, int height, List<ChatFormatting> options, Consumer<ChatFormatting> onSelect) {
        super(x, y, width, height, Component.literal(""));
        this.parent = parent;
        this.selectedOption = parent.getSelectedTeamColor();
        this.name = TeamEditScreen.teamColorsNames.get(parent.getSelectedTeamColorNameIndex()).getString();
        this.options = options;
        this.onSelect = onSelect;
        this.columns = 4;
        this.rows = 4;
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int margin = 2;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BG_FILL_SELECTED);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, name, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);


        int selectedColor = selectedOption.getColor() != null ? 0xFF000000 | selectedOption.getColor() : 0xFFFFFFFF; // Standard Weiß
        guiGraphics.fillGradient(this.getX() + margin, this.getY() + margin, this.getX() + margin + colorSize, this.getY() + margin + colorSize, selectedColor, selectedColor);

        if (isOpen) {
            int dropdownStartX = this.getX() + this.width;
            int dropdownStartY = this.getY();

            for (int i = 0; i < options.size(); i++) {
                int col = i % columns;
                int row = i / columns;

                int optionX = dropdownStartX + col * cellSize;
                int optionY = dropdownStartY + row * cellSize;

                ChatFormatting chatFormatting = options.get(i);
                int optionColor = chatFormatting.getColor() != null ? 0xFF000000 | chatFormatting.getColor() : 0xFFFFFFFF;

                if (isMouseOverOption(mouseX, mouseY, optionX, optionY))
                    guiGraphics.fill(optionX, optionY, optionX + cellSize, optionY + cellSize, BG_FILL_HOVERED);
                else
                    guiGraphics.fill(optionX, optionY, optionX + cellSize, optionY + cellSize, BG_FILL);

                guiGraphics.fillGradient(optionX + margin, optionY + margin, optionX + margin + colorSize, optionY + margin + colorSize, optionColor, optionColor);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {

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

        int dropdownStartX = this.getX() + this.width;
        int dropdownStartY = this.getY();

        int dropdownEndX = dropdownStartX + columns * cellSize;
        int dropdownEndY = dropdownStartY + rows * cellSize;

        return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionX, int optionY) {
        return mouseX >= optionX && mouseX <= optionX + cellSize && mouseY >= optionY && mouseY <= optionY + cellSize;
    }


    private void selectOption(ChatFormatting option) {
        selectedOption = option;
        onSelect.accept(option);
        isOpen = false;
    }
}
