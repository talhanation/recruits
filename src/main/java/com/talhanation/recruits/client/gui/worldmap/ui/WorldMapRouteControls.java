package com.talhanation.recruits.client.gui.worldmap.ui;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.DropDownMenu;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WorldMapRouteControls {
    private static final int ROUTE_UI_X = 10;
    private static final int ROUTE_UI_Y = 10;
    private static final int ROUTE_DROPDOWN_W = 140;
    private static final int ROUTE_BTN_SIZE = 20;
    private static final int ROUTE_BTN_GAP = 3;

    private DropDownMenu<RecruitsRoute> routeDropDown;
    private boolean claimsTransparent;

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
                route -> route == null ? "-- Route --" : route.getName(),
                routeSelector);

        routeDropDown.setBgFill(0x80333333);
        routeDropDown.setBgFillHovered(0x80555555);
        routeDropDown.setBgFillSelected(0x80222222);
    }

    public void render(
            GuiGraphics guiGraphics,
            Font font,
            RecruitsRoute selectedRoute,
            int mouseX,
            int mouseY,
            float partialTicks) {
        renderRouteDropdown(guiGraphics, font, selectedRoute, mouseX, mouseY, partialTicks);

        renderRouteButton(guiGraphics, font, mouseX, mouseY, addButtonX(), "+", false);
        renderRouteButton(
                guiGraphics, font, mouseX, mouseY, outlineButtonX(), "□", claimsTransparent);

        if (selectedRoute != null) {
            renderRouteButton(guiGraphics, font, mouseX, mouseY, editButtonX(), "⚙", false);
        }
    }

    public boolean isAddButtonHovered(double mouseX, double mouseY) {
        return isRouteButtonHovered(mouseX, mouseY, addButtonX());
    }

    public boolean isOutlineButtonHovered(double mouseX, double mouseY) {
        return isRouteButtonHovered(mouseX, mouseY, outlineButtonX());
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

    public boolean areClaimsTransparent() {
        return claimsTransparent;
    }

    public void toggleClaimsTransparency() {
        claimsTransparent = !claimsTransparent;
    }

    private static int addButtonX() {
        return ROUTE_UI_X + ROUTE_DROPDOWN_W + ROUTE_BTN_GAP;
    }

    private static int outlineButtonX() {
        return addButtonX() + ROUTE_BTN_SIZE + ROUTE_BTN_GAP;
    }

    private static int editButtonX() {
        return outlineButtonX() + ROUTE_BTN_SIZE + ROUTE_BTN_GAP;
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
            String label = selectedRoute != null ? selectedRoute.getName() : "--- Routes ---";
            guiGraphics.drawCenteredString(
                    font,
                    label,
                    ROUTE_UI_X + ROUTE_DROPDOWN_W / 2,
                    ROUTE_UI_Y + (ROUTE_BTN_SIZE - 8) / 2,
                    0xFFFFFF);
        }
    }

    private void renderRouteButton(
            GuiGraphics guiGraphics,
            Font font,
            int mouseX,
            int mouseY,
            int x,
            String label,
            boolean selected) {
        boolean hovered = isRouteButtonHovered(mouseX, mouseY, x);
        int bg = selected ? 0x80555555 : (hovered ? 0x80444444 : 0x80222222);
        int color = selected ? 0xFFFFAA00 : 0xFFFFFF;

        guiGraphics.fill(x, ROUTE_UI_Y, x + ROUTE_BTN_SIZE, ROUTE_UI_Y + ROUTE_BTN_SIZE, bg);
        guiGraphics.renderOutline(x, ROUTE_UI_Y, ROUTE_BTN_SIZE, ROUTE_BTN_SIZE, 0x40FFFFFF);
        guiGraphics.drawCenteredString(font, label, x + ROUTE_BTN_SIZE / 2, ROUTE_UI_Y + 6, color);
    }

    private static boolean isRouteButtonHovered(double mouseX, double mouseY, int x) {
        return mouseX >= x
                && mouseX <= x + ROUTE_BTN_SIZE
                && mouseY >= ROUTE_UI_Y
                && mouseY <= ROUTE_UI_Y + ROUTE_BTN_SIZE;
    }
}
