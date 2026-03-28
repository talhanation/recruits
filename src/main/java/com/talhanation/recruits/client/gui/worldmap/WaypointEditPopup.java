package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class WaypointEditPopup {

    private static final int WIDTH = 210;
    private static final int HEIGHT = 90;
    private static final int BG_COLOR = 0x80000000;
    private static final int OUTLINE_COLOR = 0x40FFFFFF;
    private static final int BTN_COLOR = 0x80222222;
    private static final int BTN_HOVERED_COLOR = 0x80444444;
    private static final int BTN_SELECTED_COLOR = 0xBB444444;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xAAAAAA;
    private static final int BTN_H = 14;
    private static final int BTN_W = 62;

    private final WorldMapScreen parent;

    private boolean visible = false;
    private RecruitsRoute.Waypoint waypoint;
    private RecruitsRoute.WaypointAction.Type actionType;
    private EditBox waitField;

    public WaypointEditPopup(WorldMapScreen parent) {
        this.parent = parent;
    }

    public boolean isVisible() {
        return visible;
    }

    public void open(RecruitsRoute.Waypoint waypoint) {
        this.waypoint = waypoint;
        this.actionType = waypoint.getAction() != null ? waypoint.getAction().getType() : null;

        String initVal = (waypoint.getAction() != null)
                ? String.valueOf(waypoint.getAction().getWaitSeconds())
                : "0";

        // Create EditBox for seconds — positioned at render time when we know px/py
        waitField = null; // rebuilt in render on first draw
        pendingWaitFieldValue = initVal;

        this.visible = true;
    }

    // Deferred: we need parent dimensions to position the EditBox
    private boolean waitFieldBuilt = false;
    private String pendingWaitFieldValue = "0";

    private void ensureWaitField() {
        if (waitFieldBuilt || actionType != RecruitsRoute.WaypointAction.Type.WAIT) return;
        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;
        int rowY = py + 22;
        int fieldY = rowY + 20;
        int fieldX = px + 66;
        int fieldW = 54;

        waitField = new EditBox(Minecraft.getInstance().font,
                fieldX, fieldY + 2, fieldW, 8, Component.empty());
        waitField.setMaxLength(5);
        waitField.setValue(pendingWaitFieldValue);
        waitField.setFilter(s -> s.chars().allMatch(Character::isDigit));
        waitField.setFocused(true);
        waitField.setBordered(false);
        waitField.setTextColor(TEXT_COLOR);
        waitFieldBuilt = true;
    }

    public void close() {
        this.visible = false;
        this.waypoint = null;
        this.waitField = null;
        this.waitFieldBuilt = false;
        this.pendingWaitFieldValue = "0";
    }

    private void confirm() {
        if (waypoint == null) { close(); return; }

        if (actionType == null) {
            waypoint.setAction(null);
        } else {
            int seconds = 0;
            if (waitField != null && !waitField.getValue().isBlank()) {
                try { seconds = Math.max(0, Integer.parseInt(waitField.getValue())); }
                catch (NumberFormatException ignored) {}
            }
            waypoint.setAction(new RecruitsRoute.WaypointAction(actionType, seconds));
        }

        ClientManager.saveRoute(parent.selectedRoute);
        close();
    }

    public void tick() {
        if (visible && waitField != null) waitField.tick();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!visible) return;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        guiGraphics.fill(0, 0, parent.width, parent.height, 0x88000000);
        guiGraphics.fill(px, py, px + WIDTH, py + HEIGHT, BG_COLOR);
        guiGraphics.renderOutline(px, py, WIDTH, HEIGHT, OUTLINE_COLOR);

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, waypoint.getName(),
                px + WIDTH / 2, py + 6, TEXT_COLOR);

        int rowY = py + 22;
        guiGraphics.drawString(Minecraft.getInstance().font, "Action:", px + 8, rowY + 3, TEXT_MUTED, false);

        renderActionButton(guiGraphics, mouseX, mouseY, "None", null, px + 56, rowY);
        renderActionButton(guiGraphics, mouseX, mouseY, "Wait", RecruitsRoute.WaypointAction.Type.WAIT, px + 56 + BTN_W + 4, rowY);

        if (actionType == RecruitsRoute.WaypointAction.Type.WAIT) {
            ensureWaitField();
            int fieldY = rowY + 20;
            guiGraphics.drawString(Minecraft.getInstance().font, "Seconds:", px + 8, fieldY + 2, TEXT_MUTED, false);
            int fieldX = px + 66;
            int fieldW = 54;
            guiGraphics.fill(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + 13, 0x80303030);
            guiGraphics.renderOutline(fieldX - 1, fieldY - 1, fieldW + 2, 14, OUTLINE_COLOR);
            if (waitField != null) waitField.render(guiGraphics, mouseX, mouseY, 0);
        }

        int btnY = py + HEIGHT - 18;
        renderButton(guiGraphics, mouseX, mouseY, "OK",     px + 8,           btnY, 80, BTN_H);
        renderButton(guiGraphics, mouseX, mouseY, "Cancel", px + WIDTH - 88,  btnY, 80, BTN_H);
    }

    private void renderActionButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                     String label, RecruitsRoute.WaypointAction.Type type,
                                     int x, int y) {
        boolean selected = actionType == type;
        boolean hovered  = mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H;
        int bg = selected ? BTN_SELECTED_COLOR : (hovered ? BTN_HOVERED_COLOR : BTN_COLOR);
        guiGraphics.fill(x, y, x + BTN_W, y + BTN_H, bg);
        guiGraphics.renderOutline(x, y, BTN_W, BTN_H, selected ? TEXT_COLOR : OUTLINE_COLOR);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label,
                x + BTN_W / 2, y + (BTN_H - 8) / 2, TEXT_COLOR);
    }

    private void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                               String label, int x, int y, int w, int h) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        guiGraphics.fill(x, y, x + w, y + h, hovered ? BTN_HOVERED_COLOR : BTN_COLOR);
        guiGraphics.renderOutline(x, y, w, h, OUTLINE_COLOR);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label,
                x + w / 2, y + (h - 8) / 2, TEXT_COLOR);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible) return false;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        if (mouseX < px || mouseX > px + WIDTH || mouseY < py || mouseY > py + HEIGHT) {
            close();
            return true;
        }

        int rowY = py + 22;

        // Action: None
        if (mouseX >= px + 56 && mouseX <= px + 56 + BTN_W && mouseY >= rowY && mouseY <= rowY + BTN_H) {
            actionType = null;
            waitField = null;
            waitFieldBuilt = false;
            return true;
        }
        // Action: Wait
        if (mouseX >= px + 56 + BTN_W + 4 && mouseX <= px + 56 + BTN_W * 2 + 4
                && mouseY >= rowY && mouseY <= rowY + BTN_H) {
            if (actionType != RecruitsRoute.WaypointAction.Type.WAIT) {
                waitField = null;
                waitFieldBuilt = false;
            }
            actionType = RecruitsRoute.WaypointAction.Type.WAIT;
            ensureWaitField();
            return true;
        }

        // Forward click to EditBox
        if (waitField != null) waitField.mouseClicked(mouseX, mouseY, 0);

        int btnY = py + HEIGHT - 18;
        if (mouseX >= px + 8 && mouseX <= px + 88 && mouseY >= btnY && mouseY <= btnY + BTN_H) {
            confirm(); return true;
        }
        if (mouseX >= px + WIDTH - 88 && mouseX <= px + WIDTH - 8 && mouseY >= btnY && mouseY <= btnY + BTN_H) {
            close(); return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close(); return true;
        }

        if (waitField != null && actionType == RecruitsRoute.WaypointAction.Type.WAIT) {
            waitField.keyPressed(keyCode, 0, 0);
        }
        return true;
    }

    public boolean charTyped(char chr) {
        if (!visible) return false;
        if (waitField != null && actionType == RecruitsRoute.WaypointAction.Type.WAIT) {
            waitField.charTyped(chr, 0);
        }
        return true;
    }
}
