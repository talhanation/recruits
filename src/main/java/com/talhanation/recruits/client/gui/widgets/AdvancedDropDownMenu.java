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

public class AdvancedDropDownMenu<T> extends AbstractWidget {
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int TEXT_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    protected static final int SCROLLBAR_COLOR = FastColor.ARGB32.color(255, 150, 150, 150);
    protected static final int SCROLLBAR_HOVERED_COLOR = FastColor.ARGB32.color(255, 200, 200, 200);

    private final List<T> options;
    private final Consumer<T> onSelect;
    private T selectedOption;
    private boolean isOpen;
    private final int optionHeight = 20;
    private final int maxVisibleOptions; // Maximum number of visible options
    private int scrollOffset = 0; // Current scroll position

    private final Function<T, String> optionTextGetter; // Function to get display text for an option

    public AdvancedDropDownMenu(
            T selectedOption, int x, int y, int width, int height,
            List<T> options, Consumer<T> onSelect,
            Function<T, String> optionTextGetter, int maxVisibleOptions
    ) {
        super(x, y, width, height, TextComponent.EMPTY);
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
        this.optionTextGetter = optionTextGetter;
        this.maxVisibleOptions = maxVisibleOptions;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        // Render the main button
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, BG_FILL_SELECTED);
        drawCenteredString(poseStack, Minecraft.getInstance().font, getSelectedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, TEXT_COLOR);

        // Render the dropdown options if open
        if (isOpen) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            int dropdownY = this.y + this.height;

            // Render the dropdown background
            fill(poseStack, this.x, dropdownY, this.x + this.width, dropdownY + dropdownHeight, BG_FILL);

            // Calculate the range of visible options based on the scroll offset
            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + maxVisibleOptions, options.size());

            // Render visible options
            for (int i = startIndex; i < endIndex; i++) {
                int optionY = dropdownY + (i - startIndex) * optionHeight;
                T option = options.get(i);

                // Highlight the option if hovered
                if (isMouseOverOption(mouseX, mouseY, optionY)) {
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, BG_FILL_HOVERED);
                }

                // Render the option's text
                String text = optionTextGetter.apply(option);
                drawCenteredString(poseStack, Minecraft.getInstance().font, text, this.x + this.width / 2, optionY + (optionHeight - 8) / 2, TEXT_COLOR);
            }

            // Render the scrollbar
            if (options.size() > maxVisibleOptions) {
                int scrollbarWidth = 4;
                int scrollbarX = this.x + this.width - scrollbarWidth;
                int scrollbarHeight = (int) ((float) maxVisibleOptions / options.size() * dropdownHeight);
                int scrollbarY = dropdownY + (int) ((float) scrollOffset / options.size() * dropdownHeight);

                fill(poseStack, scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, SCROLLBAR_COLOR);
            }
        }
    }


    public void onClick(double mouseX, double mouseY) {
        if (isOpen) {
            int dropdownY = this.y + this.height;
            for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisibleOptions, options.size()); i++) {
                int optionY = dropdownY + (i - scrollOffset) * optionHeight;

                if (isMouseOverOption((int) mouseX, (int) mouseY, optionY)) {
                    selectOption(options.get(i));
                    return;
                }
            }

            if (isMouseOverScrollbar((int) mouseX, (int) mouseY)) {
                handleScrollbarClick((int) mouseY);
                return;
            }
        }

        if (isMouseOverMainButton((int) mouseX, (int) mouseY)) {
            isOpen = !isOpen;
        }
    }


    public void onMouseMove(double mouseX, double mouseY) {
        // Close the dropdown if the mouse moves outside
        if (isOpen && !isMouseOverDropdown((int) mouseX, (int) mouseY)) {
            isOpen = false;
        }
    }


    public boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOpen) {
            // Scroll the dropdown
            scrollOffset = (int) Math.max(0, Math.min(scrollOffset - delta, options.size() - maxVisibleOptions));
            return true;
        }
        return false;
    }

    private boolean isMouseOverMainButton(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= optionY && mouseY <= optionY + optionHeight;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        if (!isOpen || options.size() <= maxVisibleOptions) return false;

        int dropdownY = this.y + this.height;
        int dropdownHeight = maxVisibleOptions * optionHeight;
        int scrollbarX = this.x + this.width - 4;

        return mouseX >= scrollbarX && mouseX <= scrollbarX + 4 && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (isMouseOverMainButton(mouseX, mouseY)) {
            return true;
        }
        if (isOpen) {
            int dropdownY = this.y + this.height;
            int dropdownHeight = maxVisibleOptions * optionHeight;

            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;
        }
        return false;
    }

    private void handleScrollbarClick(int mouseY) {
        int dropdownY = this.y + this.height;
        int dropdownHeight = maxVisibleOptions * optionHeight;
        float scrollPercentage = (float) (mouseY - dropdownY) / dropdownHeight;
        scrollOffset = (int) (scrollPercentage * (options.size() - maxVisibleOptions));
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
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        // Implement narration if needed
    }
}