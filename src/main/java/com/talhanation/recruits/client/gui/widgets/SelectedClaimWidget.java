package com.talhanation.recruits.client.gui.widgets;

import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.jetbrains.annotations.Nullable;

/**
 * Shows a selected claim like {@link SelectedPlayerWidget} shows a player:
 * faction banner, claim name and a second line, with an action button ("x") on the right.
 * Clicking the rest of the widget runs the select action (e.g. open the claim list).
 */
public class SelectedClaimWidget extends AbstractWidget {

    private static final int NAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private static final int INFO_COLOR = FastColor.ARGB32.color(255, 180, 180, 180);
    private static final int BACKGROUND_COLOR = FastColor.ARGB32.color(255, 0, 0, 0);
    private static final int BACKGROUND_HOVER = FastColor.ARGB32.color(255, 40, 40, 40);

    private final Font font;
    private final Button actionButton;
    private final Runnable onSelect;

    private String claimName = "";
    private String info = "";
    @Nullable
    private BannerRenderer bannerRenderer;

    public SelectedClaimWidget(Font font, int x, int y, int width, int height, Component buttonLabel, Runnable onButton, Runnable onSelect) {
        super(x, y, width, height, Component.literal(""));
        this.font = font;
        this.onSelect = onSelect;
        this.actionButton = new ExtendedButton(x + width - 20, y, 20, height, buttonLabel, button -> onButton.run());
    }

    public void setClaim(String claimName, String info, @Nullable RecruitsFaction faction) {
        this.claimName = claimName == null ? "" : claimName;
        this.info = info == null ? "" : info;
        this.bannerRenderer = faction != null ? new BannerRenderer(faction) : null;
    }

    public void setButtonTooltip(Component tooltip) {
        this.actionButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hoverBody = isHovered() && !actionButton.isMouseOver(mouseX, mouseY);

        guiGraphics.fill(x, y, x + w, y + h, hoverBody ? BACKGROUND_HOVER : BACKGROUND_COLOR);
        if (bannerRenderer != null) {
            bannerRenderer.renderBanner(guiGraphics, x, y, 20, h, 10);
        }

        int textX = x + 22;
        if (info.isEmpty()) {
            guiGraphics.drawString(font, claimName, textX, y + (h - font.lineHeight) / 2, NAME_COLOR, false);
        } else {
            guiGraphics.drawString(font, claimName, textX, y + 2, NAME_COLOR, false);
            guiGraphics.drawString(font, info, textX, y + 2 + font.lineHeight + 1, INFO_COLOR, false);
        }

        actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.visible || !this.isMouseOver(mx, my) || button != 0) return false;
        if (actionButton.isMouseOver(mx, my)) {
            if (actionButton.active && actionButton.visible) actionButton.onClick(mx, my);
        } else {
            onSelect.run();
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
