package com.talhanation.recruits.client.gui.worldmap.ui;

import com.talhanation.recruits.config.RecruitsClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class WorldMapSettingsPanel {
    private static final int MARGIN = 10;
    private static final int BUTTON_SIZE = 20;
    private static final int PANEL_WIDTH = 178;
    private static final int PANEL_HEIGHT = 134;
    private static final int PANEL_TOP = MARGIN + BUTTON_SIZE + 4;
    private static final int ROW_START = 24;
    private static final int ROW_HEIGHT = 20;
    private static final int CHECKBOX_SIZE = 10;
    private static final int BG_COLOR = 0xAA101010;
    private static final int BUTTON_COLOR = 0x80222222;
    private static final int BUTTON_HOVERED_COLOR = 0x80444444;
    private static final int SELECTED_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int OUTLINE_COLOR = 0x40FFFFFF;
    private static final String CLOSE_ICON = "✕";
    private static final String SETTINGS_ICON = "⚙";
    private static final Component TITLE = Component.translatable("gui.recruits.map.settings.title");
    private static final Component LOAD_PLAYER_AREA = Component.translatable("gui.recruits.map.settings.load_player_area");
    private static final Component LOAD_VIEWED_MAP = Component.translatable("gui.recruits.map.settings.load_viewed_map");
    private static final Component PLAYER_ARROW = Component.translatable("gui.recruits.map.settings.player_arrow");
    private static final Component NIGHT_SHADING = Component.translatable("gui.recruits.map.settings.night_shading");
    private static final Component COORDINATES = Component.translatable("gui.recruits.map.settings.coordinates");

    private boolean open;

    public void render(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        renderToggleButton(guiGraphics, font, screenWidth, mouseX, mouseY);
        if (!open) return;

        int panelX = panelX(screenWidth);
        int panelHeight = panelHeight(screenHeight);
        guiGraphics.fill(panelX, PANEL_TOP, panelX + PANEL_WIDTH, PANEL_TOP + panelHeight, BG_COLOR);
        guiGraphics.renderOutline(panelX, PANEL_TOP, PANEL_WIDTH, panelHeight, OUTLINE_COLOR);
        guiGraphics.drawString(font, TITLE, panelX + 8, PANEL_TOP + 7, TEXT_COLOR, false);

        renderCheckBox(
                guiGraphics,
                font,
                panelX,
                PANEL_TOP + ROW_START,
                LOAD_PLAYER_AREA,
                RecruitsClientConfig.WorldMapUpdateAroundPlayer.get(),
                isRowHovered(mouseX, mouseY, 0, screenWidth, screenHeight));
        renderCheckBox(
                guiGraphics,
                font,
                panelX,
                PANEL_TOP + ROW_START + ROW_HEIGHT,
                LOAD_VIEWED_MAP,
                RecruitsClientConfig.WorldMapLoadViewedRegions.get(),
                isRowHovered(mouseX, mouseY, 1, screenWidth, screenHeight));
        renderCheckBox(
                guiGraphics,
                font,
                panelX,
                PANEL_TOP + ROW_START + ROW_HEIGHT * 2,
                PLAYER_ARROW,
                usePlayerArrow(),
                isRowHovered(mouseX, mouseY, 2, screenWidth, screenHeight));
        renderCheckBox(
                guiGraphics,
                font,
                panelX,
                PANEL_TOP + ROW_START + ROW_HEIGHT * 3,
                NIGHT_SHADING,
                RecruitsClientConfig.WorldMapNightShading.get(),
                isRowHovered(mouseX, mouseY, 3, screenWidth, screenHeight));
        renderCheckBox(
                guiGraphics,
                font,
                panelX,
                PANEL_TOP + ROW_START + ROW_HEIGHT * 4,
                COORDINATES,
                RecruitsClientConfig.WorldMapShowCoordinates.get(),
                isRowHovered(mouseX, mouseY, 4, screenWidth, screenHeight));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
        if (button != 0) return isMouseBlocking(mouseX, mouseY, screenWidth, screenHeight);

        if (isToggleButtonHovered(mouseX, mouseY, screenWidth)) {
            open = !open;
            return true;
        }
        if (!open) return false;
        if (!isPanelHovered(mouseX, mouseY, screenWidth, screenHeight)) return false;

        if (isRowHovered(mouseX, mouseY, 0, screenWidth, screenHeight)) {
            RecruitsClientConfig.WorldMapUpdateAroundPlayer.set(
                    !RecruitsClientConfig.WorldMapUpdateAroundPlayer.get());
        } else if (isRowHovered(mouseX, mouseY, 1, screenWidth, screenHeight)) {
            RecruitsClientConfig.WorldMapLoadViewedRegions.set(
                    !RecruitsClientConfig.WorldMapLoadViewedRegions.get());
        } else if (isRowHovered(mouseX, mouseY, 2, screenWidth, screenHeight)) {
            togglePlayerIconStyle();
        } else if (isRowHovered(mouseX, mouseY, 3, screenWidth, screenHeight)) {
            RecruitsClientConfig.WorldMapNightShading.set(!RecruitsClientConfig.WorldMapNightShading.get());
        } else if (isRowHovered(mouseX, mouseY, 4, screenWidth, screenHeight)) {
            RecruitsClientConfig.WorldMapShowCoordinates.set(!RecruitsClientConfig.WorldMapShowCoordinates.get());
        }
        return true;
    }

    public boolean isMouseBlocking(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        return isToggleButtonHovered(mouseX, mouseY, screenWidth)
                || open && isPanelHovered(mouseX, mouseY, screenWidth, screenHeight);
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public boolean usePlayerArrow() {
        return RecruitsClientConfig.WorldMapPlayerIconStyle.get()
                == RecruitsClientConfig.MapPlayerIconStyle.OVERHAULED;
    }

    private void togglePlayerIconStyle() {
        RecruitsClientConfig.MapPlayerIconStyle next = usePlayerArrow()
                ? RecruitsClientConfig.MapPlayerIconStyle.VANILLA
                : RecruitsClientConfig.MapPlayerIconStyle.OVERHAULED;
        RecruitsClientConfig.WorldMapPlayerIconStyle.set(next);
    }

    private void renderToggleButton(GuiGraphics guiGraphics, Font font, int screenWidth, int mouseX, int mouseY) {
        int x = toggleButtonX(screenWidth);
        boolean hovered = isToggleButtonHovered(mouseX, mouseY, screenWidth);
        int color = hovered || open ? BUTTON_HOVERED_COLOR : BUTTON_COLOR;
        guiGraphics.fill(x, MARGIN, x + BUTTON_SIZE, MARGIN + BUTTON_SIZE, color);
        guiGraphics.renderOutline(x, MARGIN, BUTTON_SIZE, BUTTON_SIZE, OUTLINE_COLOR);
        guiGraphics.drawCenteredString(
                font, open ? CLOSE_ICON : SETTINGS_ICON, x + BUTTON_SIZE / 2, MARGIN + 6, TEXT_COLOR);
    }

    private void renderCheckBox(
            GuiGraphics guiGraphics, Font font, int panelX, int rowY, Component label, boolean checked, boolean hovered) {
        if (hovered) {
            guiGraphics.fill(panelX + 4, rowY - 2, panelX + PANEL_WIDTH - 4, rowY + ROW_HEIGHT - 2, 0x30444444);
        }

        int boxX = panelX + 8;
        int boxY = rowY + 3;
        guiGraphics.fill(boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE, 0x80222222);
        guiGraphics.renderOutline(boxX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE, OUTLINE_COLOR);
        if (checked) {
            guiGraphics.fill(boxX + 2, boxY + 2, boxX + CHECKBOX_SIZE - 2, boxY + CHECKBOX_SIZE - 2, SELECTED_COLOR);
        }

        guiGraphics.drawString(font, label, boxX + CHECKBOX_SIZE + 7, rowY + 4, TEXT_COLOR, false);
    }

    private static boolean isToggleButtonHovered(double mouseX, double mouseY, int screenWidth) {
        int x = toggleButtonX(screenWidth);
        return mouseX >= x
                && mouseX <= x + BUTTON_SIZE
                && mouseY >= MARGIN
                && mouseY <= MARGIN + BUTTON_SIZE;
    }

    private static boolean isPanelHovered(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int panelX = panelX(screenWidth);
        int panelHeight = panelHeight(screenHeight);
        return mouseX >= panelX
                && mouseX <= panelX + PANEL_WIDTH
                && mouseY >= PANEL_TOP
                && mouseY <= PANEL_TOP + panelHeight;
    }

    private static boolean isRowHovered(double mouseX, double mouseY, int row, int screenWidth, int screenHeight) {
        int panelX = panelX(screenWidth);
        int rowY = PANEL_TOP + ROW_START + ROW_HEIGHT * row;
        return mouseX >= panelX + 4
                && mouseX <= panelX + PANEL_WIDTH - 4
                && mouseY >= rowY - 2
                && mouseY <= rowY + ROW_HEIGHT - 2
                && isPanelHovered(mouseX, mouseY, screenWidth, screenHeight);
    }

    private static int toggleButtonX(int screenWidth) {
        return screenWidth - MARGIN - BUTTON_SIZE;
    }

    private static int panelX(int screenWidth) {
        return screenWidth - MARGIN - PANEL_WIDTH;
    }

    private static int panelHeight(int screenHeight) {
        return Math.min(PANEL_HEIGHT, Math.max(0, screenHeight - PANEL_TOP - MARGIN));
    }
}
