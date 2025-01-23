package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropDownMenu<T> extends AbstractWidget {
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    protected static final int TEXT_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private final List<T> options;
    private final Consumer<T> onSelect;
    Function<T, String> optionTextGetter;
    private T selectedOption;
    private boolean isOpen;
    private final int optionHeight;

    public DropDownMenu(T selectedOption, int x, int y, int width, int height, List<T> options, Function<T, String> optionTextGetter, Consumer<T> onSelect) {
        super(x, y, width, height, TextComponent.EMPTY);
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
        this.optionTextGetter = optionTextGetter;
        this.optionHeight = height;
    }

    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, BG_FILL_SELECTED);
        drawCenteredString(poseStack, Minecraft.getInstance().font, getSelectedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, PLAYER_NAME_COLOR);

        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + i * optionHeight;

                T option = options.get(i);

                if (isMouseOverOption(mouseX, mouseY, optionY))
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, BG_FILL_HOVERED);
                else
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, BG_FILL);

                String text = optionTextGetter.apply(option);
                drawCenteredString(poseStack, Minecraft.getInstance().font, text, this.x + this.width/2, optionY + (optionHeight - 8) / 2, TEXT_COLOR);
            }

        }
    }
    public void onClick(double mouseX, double mouseY) {
        //Do not use
    }

    public void onMouseClick(double mouseX, double mouseY) {
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + i * optionHeight;

                if (isMouseOverOption((int) mouseX, (int) mouseY, optionY)) {
                    selectOption(options.get(i));
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
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (!isOpen) return false;

        int dropdownStartX = this.x;
        int dropdownStartY = this.y;

        int dropdownEndX = dropdownStartX + this.width;
        int dropdownEndY = dropdownStartY + options.size() * optionHeight;

        return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= optionY && mouseY <= optionY + optionHeight;
    }

    private void selectOption(T option) {
        selectedOption = option;
        onSelect.accept(option);
        isOpen = false;
    }

    private String getSelectedText() {
        return selectedOption != null ? optionTextGetter.apply(selectedOption) : "";
    }
    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
