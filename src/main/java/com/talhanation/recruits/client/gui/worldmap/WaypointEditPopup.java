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
    private static final int HEIGHT = 110;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xAAAAAA;
    private static final int BTN_H = 14;
    private static final int BTN_W = 62;
    private static final Component TEXT_TITLE = Component.translatable("gui.recruits.map.waypoint.title");
    private static final Component TEXT_ACTION = Component.translatable("gui.recruits.map.waypoint.action");
    private static final Component TEXT_ACTION_NONE = Component.translatable("gui.recruits.map.waypoint.action.none");
    private static final Component TEXT_ACTION_WAIT = Component.translatable("gui.recruits.map.waypoint.action.wait");
    private static final Component TEXT_SECONDS = Component.translatable("gui.recruits.map.waypoint.seconds");
    private static final Component TEXT_HINT_NONE = Component.translatable("gui.recruits.map.waypoint.hint.none");
    private static final Component TEXT_HINT_WAIT = Component.translatable("gui.recruits.map.waypoint.hint.wait");
    private static final Component TEXT_CONFIRM = Component.translatable("gui.recruits.map.common.ok");
    private static final Component TEXT_CANCEL = Component.translatable("gui.recruits.map.common.cancel");
    private static final Component TEXT_FEEDBACK_SAVED = Component.translatable("gui.recruits.map.waypoint.feedback.saved");
    private static final Component TEXT_FEEDBACK_CLEARED = Component.translatable("gui.recruits.map.waypoint.feedback.cleared");

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
        int rowY = py + 48;
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
        parent.showMapNotice(actionType == null ? TEXT_FEEDBACK_CLEARED : TEXT_FEEDBACK_SAVED, 0xFF9FDB6B);
        close();
    }

    private Component getHintText() {
        return actionType == RecruitsRoute.WaypointAction.Type.WAIT ? TEXT_HINT_WAIT : TEXT_HINT_NONE;
    }

    public void tick() {
        if (visible && waitField != null) waitField.tick();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!visible) return;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        guiGraphics.fill(0, 0, parent.width, parent.height, 0x88000000);
        WorldMapRenderPrimitives.panel(guiGraphics, px, py, WIDTH, HEIGHT);

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, TEXT_TITLE,
                px + WIDTH / 2, py + 6, TEXT_COLOR);
        guiGraphics.drawString(Minecraft.getInstance().font, waypoint.getName(), px + 8, py + 18, 0xFFE6D6A8, false);
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, getHintText(), px + 8, py + 30, WIDTH - 16, 0xFFB8A17A);

        int rowY = py + 48;
        guiGraphics.drawString(Minecraft.getInstance().font, TEXT_ACTION, px + 8, rowY + 3, TEXT_MUTED, false);

        renderActionButton(guiGraphics, mouseX, mouseY, TEXT_ACTION_NONE, null, px + 56, rowY);
        renderActionButton(guiGraphics, mouseX, mouseY, TEXT_ACTION_WAIT, RecruitsRoute.WaypointAction.Type.WAIT, px + 56 + BTN_W + 4, rowY);

        if (actionType == RecruitsRoute.WaypointAction.Type.WAIT) {
            ensureWaitField();
            int fieldY = rowY + 20;
            guiGraphics.drawString(Minecraft.getInstance().font, TEXT_SECONDS, px + 8, fieldY + 2, TEXT_MUTED, false);
            int fieldX = px + 66;
            int fieldW = 54;
            guiGraphics.fill(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + 13, 0x80303030);
            guiGraphics.renderOutline(fieldX - 1, fieldY - 1, fieldW + 2, 14, 0xAA8A6A3A);
            if (waitField != null) waitField.render(guiGraphics, mouseX, mouseY, 0);
        }

        int btnY = py + HEIGHT - 18;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_CONFIRM, px + 8,           btnY, 80, BTN_H);
        renderButton(guiGraphics, mouseX, mouseY, TEXT_CANCEL, px + WIDTH - 88,  btnY, 80, BTN_H);
    }

    private void renderActionButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                     Component label, RecruitsRoute.WaypointAction.Type type,
                                     int x, int y) {
        WorldMapRenderPrimitives.button(guiGraphics, Minecraft.getInstance().font, mouseX, mouseY,
                x, y, BTN_W, BTN_H, label, TEXT_COLOR, actionType == type, true);
    }

    private void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                               Component label, int x, int y, int w, int h) {
        WorldMapRenderPrimitives.button(guiGraphics, Minecraft.getInstance().font, mouseX, mouseY,
                x, y, w, h, label, TEXT_COLOR, false, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible) return false;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        if (mouseX < px || mouseX > px + WIDTH || mouseY < py || mouseY > py + HEIGHT) {
            close();
            return true;
        }

        int rowY = py + 48;

        // Action: None
        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + 56, rowY, BTN_W, BTN_H)) {
            actionType = null;
            waitField = null;
            waitFieldBuilt = false;
            return true;
        }
        // Action: Wait
        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + 56 + BTN_W + 4, rowY, BTN_W, BTN_H)) {
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
        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + 8, btnY, 80, BTN_H)) {
            confirm(); return true;
        }
        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + WIDTH - 88, btnY, 80, BTN_H)) {
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
