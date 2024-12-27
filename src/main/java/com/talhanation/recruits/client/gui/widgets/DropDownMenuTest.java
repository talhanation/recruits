package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.function.Consumer;

public class DropDownMenuTest extends AbstractWidget {
    protected static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    protected static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    protected static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    protected static final int PLAYER_NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private final List<ChatFormatting> options;
    private final Consumer<ChatFormatting> onSelect;
    private ChatFormatting selectedOption;
    private boolean isOpen;
    private final int optionHeight = 20;

    public DropDownMenuTest(ChatFormatting selectedOption, int x, int y, int width, int height, List<ChatFormatting> options, Consumer<ChatFormatting> onSelect) {
        super(x, y, width, height, TextComponent.EMPTY);
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
    }

    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, BG_FILL_SELECTED);
        drawCenteredString(poseStack, Minecraft.getInstance().font, getSelectedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, PLAYER_NAME_COLOR);
        fillGradient(poseStack, this.x + 4, this.y + 4, this.x + 16, this.y + 16, selectedOption.getColor(), selectedOption.getColor());

        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + i * optionHeight;
                ChatFormatting chatFormatting = options.get(i);

                if (isMouseOverOption(mouseX, mouseY, optionY))
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, BG_FILL_HOVERED);
                else
                    fill(poseStack, this.x, optionY, this.x + this.width, optionY + optionHeight, BG_FILL);

                if(chatFormatting.getColor() != null){
                    fillGradient(poseStack, this.x + 4, optionY + 4, this.x + 16, optionY + optionHeight + 16, chatFormatting.getColor(), chatFormatting.getColor());
                }

            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + i * optionHeight;

                if (isMouseOverOption((int) mouseX, (int) mouseY, optionY)) {
                    selectOption(options.get(i));
                    return;
                }
            }
        }
        isOpen = !isOpen;
    }


    public void onMouseMove(double mouseX, double mouseY) {
        if (isOpen && !isMouseOverDropdown((int) mouseX, (int) mouseY)) {
            isOpen = false;
        }
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= optionY && mouseY <= optionY + optionHeight;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
            return true;
        }
        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.y + this.height + i * optionHeight;
                if (isMouseOverOption(mouseX, mouseY, optionY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void selectOption(ChatFormatting option) {
        selectedOption = option;
        onSelect.accept(option);
        isOpen = false;
    }

    private String getSelectedText() {
        return  selectedOption != null ? selectedOption.getName() : "";
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
