package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public class ScrollDropDownMenu<T> extends AbstractWidget {
    private int bgFill = FastColor.ARGB32.color(255, 60, 60, 60);
    private int bgFillHovered = FastColor.ARGB32.color(255, 100, 100, 100);
    private int bgFillSelected = FastColor.ARGB32.color(255, 10, 10, 10);
    private int displayColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int optionTextColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int scrollbarColor = FastColor.ARGB32.color(255, 150, 150, 150);
    private int scrollbarHandleColor = FastColor.ARGB32.color(255, 200, 200, 200);

    private final List<T> options;
    private final Consumer<T> onSelect;
    private final Function<T, String> optionTextGetter;
    private T selectedOption;
    private boolean isOpen;
    private final int optionHeight;

    // Scroll-related fields
    private int scrollOffset = 0; // Tracks how far the list is scrolled
    private int maxVisibleOptions; // Maximum number of options visible at once
    private boolean isScrolling = false; // Whether the scrollbar is being dragged
    private int scrollbarWidth = 6; // Width of the scrollbar
    private int scrollbarHandleHeight; // Height of the scrollbar handle

    public ScrollDropDownMenu(T selectedOption, int x, int y, int width, int height, List<T> options, Function<T, String> optionTextGetter, Consumer<T> onSelect) {
        super(x, y, width, height, Component.literal(""));
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
        this.optionTextGetter = optionTextGetter;
        this.optionHeight = height;
        this.maxVisibleOptions = Math.min(5, options.size()); // Adjust based on your needs
        this.scrollbarHandleHeight = Math.max(10, (int) ((float) maxVisibleOptions / options.size() * (height * maxVisibleOptions)));
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        if(!visible) return;

        if (isMouseOverDisplay(mouseX, mouseY)) {
            fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, bgFillHovered);
        } else {
            fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, bgFillSelected);
        }

        drawCenteredString(poseStack, Minecraft.getInstance().font, getSelectedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, displayColor);

        if (isOpen) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            fill(poseStack, this.x, this.y + this.height, this.x + this.width, this.y + this.height + dropdownHeight, bgFill);

            poseStack.pushPose();
            poseStack.translate(0, 0, 500); // Ensure the dropdown renders above other elements
            RenderSystem.enableScissor((int) (this.x * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (Minecraft.getInstance().getWindow().getHeight() - (this.y + this.height + dropdownHeight) * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (this.width * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (dropdownHeight * Minecraft.getInstance().getWindow().getGuiScale()));

            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + (i - scrollOffset) * optionHeight;

                if (isMouseOverOption(mouseX, mouseY, optionY)) {
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, bgFillHovered);
                } else {
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, bgFill);
                }

                String text = optionTextGetter.apply(options.get(i));
                drawCenteredString(poseStack, Minecraft.getInstance().font, text, this.x + this.width / 2, optionY + (optionHeight - 8) / 2, optionTextColor);
            }

            RenderSystem.disableScissor();
            poseStack.popPose();

            // Render the scrollbar
            if (options.size() > maxVisibleOptions) {
                int scrollbarX = this.x + this.width - scrollbarWidth;
                int scrollbarY = this.y + this.height + (int) ((float) scrollOffset / options.size() * dropdownHeight);
                int scrollbarHeight = (int) ((float) maxVisibleOptions / options.size() * dropdownHeight);

                // Scrollbar background
                fill(poseStack, scrollbarX, this.y + this.height, scrollbarX + scrollbarWidth, this.y + this.height + dropdownHeight, scrollbarColor);

                // Scrollbar handle
                fill(poseStack, scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, scrollbarHandleColor);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Do not use
    }

    public void onMouseClick(double mouseX, double mouseY) {
        if(!visible) return;

        if (isOpen) {
            // Check if the click is on the scrollbar
            if (isMouseOverScrollbar((int) mouseX, (int) mouseY)) {
                isScrolling = true;
                return;
            }

            // Check if the click is on an option
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + (i - scrollOffset) * optionHeight;

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
        if(!visible) return;

        if (isOpen) {
            boolean isOverDropdown = isMouseOverDropdown((int) mouseX, (int) mouseY);
            boolean isOverDisplay = isMouseOverDisplay((int) mouseX, (int) mouseY);

            if (!isOverDropdown && !isOverDisplay) {
                isOpen = false;
            }
        }

        // Handle scrollbar dragging
        if (isScrolling) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            int scrollbarY = (int) mouseY - (this.y + this.height);
            scrollOffset = (int) ((float) scrollbarY / dropdownHeight * options.size());
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleOptions));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(!visible) return false;

        if (isOpen) {
            scrollOffset -= (int) delta;
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleOptions));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(!visible) return false;
        
        if (isScrolling) {
            isScrolling = false;
            return true;
        }
        return false;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        if (!isOpen || options.size() <= maxVisibleOptions) return false;

        int scrollbarX = this.x + this.width - scrollbarWidth;
        int scrollbarY = this.y + this.height;
        int scrollbarHeight = maxVisibleOptions * optionHeight;

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
    }

    private boolean isMouseOverDisplay(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (!isOpen) return false;

        int dropdownStartX = this.x;
        int dropdownStartY = this.y + this.height;
        int dropdownEndX = dropdownStartX + this.width;
        int dropdownEndY = dropdownStartY + maxVisibleOptions * optionHeight;

        return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= optionY && mouseY <= optionY + optionHeight;
    }

    public boolean isMouseOver(double x, double y) {
        return isMouseOverDisplay((int) x, (int) y) || isMouseOverDropdown((int) x, (int) y) || isMouseOverScrollbar((int) x, (int) y) || super.isMouseOver(x,y);
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

    public void setBgFill(int bgFill) {
        this.bgFill = bgFill;
    }

    public void setBgFillHovered(int bgFillHovered) {
        this.bgFillHovered = bgFillHovered;
    }

    public void setBgFillSelected(int bgFillSelected) {
        this.bgFillSelected = bgFillSelected;
    }

    public void setDisplayColor(int displayColor) {
        this.displayColor = displayColor;
    }

    public void setOptionTextColor(int optionTextColor) {
        this.optionTextColor = optionTextColor;
    }
}