package com.talhanation.recruits.client.gui.worldmap.ui;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.DropDownMenu;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WorldMapRouteControls {
    private static final int ROUTE_UI_X = 10;
    private static final int ROUTE_UI_Y = 10;
    private static final int ROUTE_DROPDOWN_W = 140;
    private static final int ROUTE_BTN_SIZE = 20;
    private static final int ROUTE_BTN_GAP = 3;
    private static final Component ROUTES_LABEL = Component.translatable("gui.recruits.map.route.selector");
    private static final Component EMPTY_ROUTE_LABEL = Component.translatable("gui.recruits.map.route.none");
    private static final String ADD_LABEL = "+";
    private static final String EDIT_LABEL = "⚙";

    private DropDownMenu<RecruitsRoute> routeDropDown;
    private Font cachedFont;
    private String cachedCollapsedLabel = "";
    private int cachedCollapsedLabelX;
    private int cachedAddLabelX;
    private int cachedEditLabelX;
    private boolean labelLayoutDirty = true;

    public void refresh(RecruitsRoute selectedRoute, Consumer<RecruitsRoute> routeSelector) {
        List<RecruitsRoute> routes = ClientManager.getRoutesList();
        List<RecruitsRoute> options = new ArrayList<>();
        options.add(null);
        options.addAll(routes);

        routeDropDown = new DropDownMenu<>(
                selectedRoute,
                ROUTE_UI_X,
                ROUTE_UI_Y,
                ROUTE_DROPDOWN_W,
                ROUTE_BTN_SIZE,
                options,
                route -> route == null ? EMPTY_ROUTE_LABEL.getString() : route.getName(),
                routeSelector);

        routeDropDown.setBgFill(0x80333333);
        routeDropDown.setBgFillHovered(0x80555555);
        routeDropDown.setBgFillSelected(0x80222222);
        labelLayoutDirty = true;
    }

    public void render(
            GuiGraphics guiGraphics,
            Font font,
            RecruitsRoute selectedRoute,
            int mouseX,
            int mouseY,
            float partialTicks) {
        updateLabelLayout(font, selectedRoute);
        renderRouteDropdown(guiGraphics, font, selectedRoute, mouseX, mouseY, partialTicks);

        renderRouteButton(guiGraphics, mouseX, mouseY, addButtonX(), ADD_LABEL, cachedAddLabelX, false);

        if (selectedRoute != null) {
            renderRouteButton(guiGraphics, mouseX, mouseY, editButtonX(), EDIT_LABEL, cachedEditLabelX, false);
        }
    }

    public boolean isAddButtonHovered(double mouseX, double mouseY) {
        return isRouteButtonHovered(mouseX, mouseY, addButtonX());
    }

    public boolean isEditButtonHovered(double mouseX, double mouseY) {
        return isRouteButtonHovered(mouseX, mouseY, editButtonX());
    }

    public boolean isDropdownHovered(double mouseX, double mouseY) {
        return routeDropDown != null && routeDropDown.isMouseOver(mouseX, mouseY);
    }

    public void clickDropdown(double mouseX, double mouseY) {
        if (routeDropDown != null) {
            routeDropDown.onMouseClick(mouseX, mouseY);
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        if (routeDropDown != null) {
            routeDropDown.onMouseMove(mouseX, mouseY);
        }
    }

    private static int addButtonX() {
        return ROUTE_UI_X + ROUTE_DROPDOWN_W + ROUTE_BTN_GAP;
    }

    private static int editButtonX() {
        return addButtonX() + ROUTE_BTN_SIZE + ROUTE_BTN_GAP;
    }

    private void renderRouteDropdown(
            GuiGraphics guiGraphics,
            Font font,
            RecruitsRoute selectedRoute,
            int mouseX,
            int mouseY,
            float partialTicks) {
        if (routeDropDown == null) return;

        guiGraphics.fill(
                ROUTE_UI_X,
                ROUTE_UI_Y,
                ROUTE_UI_X + ROUTE_DROPDOWN_W,
                ROUTE_UI_Y + ROUTE_BTN_SIZE,
                0x80222222);
        guiGraphics.renderOutline(ROUTE_UI_X, ROUTE_UI_Y, ROUTE_DROPDOWN_W, ROUTE_BTN_SIZE, 0x40FFFFFF);

        if (routeDropDown.isOpen()) {
            routeDropDown.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            guiGraphics.drawString(
                    font, cachedCollapsedLabel, cachedCollapsedLabelX, ROUTE_UI_Y + (ROUTE_BTN_SIZE - 8) / 2, 0xFFFFFF);
        }
    }

    private void renderRouteButton(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int x,
            String label,
            int labelX,
            boolean selected) {
        boolean hovered = isRouteButtonHovered(mouseX, mouseY, x);
        int bg = selected ? 0x80555555 : (hovered ? 0x80444444 : 0x80222222);
        int color = selected ? 0xFFFFAA00 : 0xFFFFFF;

        guiGraphics.fill(x, ROUTE_UI_Y, x + ROUTE_BTN_SIZE, ROUTE_UI_Y + ROUTE_BTN_SIZE, bg);
        guiGraphics.renderOutline(x, ROUTE_UI_Y, ROUTE_BTN_SIZE, ROUTE_BTN_SIZE, 0x40FFFFFF);
        guiGraphics.drawString(cachedFont, label, labelX, ROUTE_UI_Y + 6, color);
    }

    private void updateLabelLayout(Font font, RecruitsRoute selectedRoute) {
        String label = selectedRoute != null ? selectedRoute.getName() : ROUTES_LABEL.getString();
        if (label == null) label = "";
        if (!labelLayoutDirty && font == cachedFont && label.equals(cachedCollapsedLabel)) return;

        cachedFont = font;
        cachedCollapsedLabel = label;
        cachedCollapsedLabelX = ROUTE_UI_X + (ROUTE_DROPDOWN_W - font.width(cachedCollapsedLabel)) / 2;
        cachedAddLabelX = addButtonX() + (ROUTE_BTN_SIZE - font.width(ADD_LABEL)) / 2;
        cachedEditLabelX = editButtonX() + (ROUTE_BTN_SIZE - font.width(EDIT_LABEL)) / 2;
        labelLayoutDirty = false;
    }

    private static boolean isRouteButtonHovered(double mouseX, double mouseY, int x) {
        return mouseX >= x
                && mouseX <= x + ROUTE_BTN_SIZE
                && mouseY >= ROUTE_UI_Y
                && mouseY <= ROUTE_UI_Y + ROUTE_BTN_SIZE;
    }
}
