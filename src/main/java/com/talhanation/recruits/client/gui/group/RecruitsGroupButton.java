package com.talhanation.recruits.client.gui.group;

import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
@OnlyIn(Dist.CLIENT)
public class RecruitsGroupButton extends ExtendedButton {

    private RecruitsGroup group;

    public RecruitsGroupButton(RecruitsGroup group, int xPos, int yPos, int width, int height, Component displayString, OnPress handler) {
        super(xPos, yPos, width, height, displayString, handler);
        this.group = group;
    }

    private static Component createDisplayString(RecruitsGroup group) {
        return Component.literal(group.getName() + " (" + group.getCount() + ")");
    }

    @Override
    public boolean mouseClicked(double p_93641_, double p_93642_, int p_93643_) {
        if (this.visible) {
            if (this.isValidClickButton(p_93643_)) {
                boolean flag = this.clicked(p_93641_, p_93642_);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(p_93641_, p_93642_);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected boolean clicked(double p_93681_, double p_93682_) {
        return this.visible && p_93681_ >= (double)this.getX() && p_93682_ >= (double)this.getY() && p_93681_ < (double)(this.getX() + this.width) && p_93682_ < (double)(this.getY() + this.height);
    }

    public RecruitsGroup getGroup() {
        return group;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        int k = !this.active ? 0 : (this.isHoveredOrFocused() ? 2 : 1);
        guiGraphics.blitWithBorder(WIDGETS_LOCATION, this.getX(), this.getY(), 0, 46 + k * 20, this.width, this.height, 200, 20, 2, 3, 2, 2);

        // Get the group name and count
        String groupName = group.getName();
        String groupCount = "[" + group.getCount() + "]";

        // Set the scale for the text
        float scale = 0.8f;

        // Calculate positions for the texts
        int nameWidth = (int)(mc.font.width(groupName) * scale);
        int countWidth = (int)(mc.font.width(groupCount) * scale);

        // Calculate x positions to center the text
        int nameX = this.getX() + (this.width - nameWidth) / 2;
        int countX = this.getX() + (this.width - countWidth) / 2;

        // Calculate y positions for the texts
        int nameY = this.getY() + 2; // 2 pixels from the top
        int countY = this.getY() + (int)(mc.font.lineHeight * scale) + 4; // Below the group name with some padding

        // Draw the texts with scaling
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(nameX, nameY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(mc.font, Language.getInstance().getVisualOrder(FormattedText.of(groupName)), 0, 0, getFGColor(), false);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(countX, countY, 0);
        guiGraphics.pose().scale(1, 1, 1.0f);
        guiGraphics.drawString(mc.font, Language.getInstance().getVisualOrder(FormattedText.of(groupCount)), 0, 0, getFGColor(), false);
        guiGraphics.pose().popPose();
    }


}
