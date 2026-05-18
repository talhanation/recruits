package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class RouteNamePopup {

    private static final int WIDTH = 260;
    private static final int HEIGHT = 122;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final Component TEXT_TITLE = Component.translatable("gui.recruits.map.route.create.title");
    private static final Component TEXT_HINT = Component.translatable("gui.recruits.map.route.create.hint");
    private static final Component TEXT_NAME = Component.translatable("gui.recruits.map.route.name");
    private static final Component TEXT_CONFIRM = Component.translatable("gui.recruits.map.route.create.confirm");
    private static final Component TEXT_CANCEL = Component.translatable("gui.recruits.map.common.cancel");
    private static final Component TEXT_DISABLED_BLANK = Component.translatable("gui.recruits.map.route.disabled.blank_name");
    private static final Component TEXT_FEEDBACK_CREATED = Component.translatable("gui.recruits.map.route.feedback.created");

    private final WorldMapScreen parent;

    private boolean visible = false;
    private EditBox nameField;

    public RouteNamePopup(WorldMapScreen parent) {
        this.parent = parent;
    }

    public boolean isVisible() {
        return visible;
    }

    public void open() {
        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;
        int fieldX = px + 8;
        int fieldY = py + 58;
        int fieldW = WIDTH - 16;

        nameField = new EditBox(Minecraft.getInstance().font,
                fieldX, fieldY + 3, fieldW, 8, Component.empty());
        nameField.setMaxLength(32);
        nameField.setValue("");
        nameField.setFocused(true);
        nameField.setBordered(false);
        nameField.setTextColor(TEXT_COLOR);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.nameField = null;
    }

    private void confirm() {
        if (!canConfirm()) {
            return;
        }

        RecruitsRoute newRoute = new RecruitsRoute(nameField.getValue().trim());
        ClientManager.saveRoute(newRoute);
        parent.selectedRoute = newRoute;
        parent.refreshRouteUI();
        parent.showMapNotice(TEXT_FEEDBACK_CREATED, 0xFF9FDB6B);
        close();
    }

    private boolean canConfirm() {
        return nameField != null && !nameField.getValue().trim().isBlank();
    }

    public void tick() {
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!visible) return;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        guiGraphics.fill(0, 0, parent.width, parent.height, 0x88000000);
        WorldMapRenderPrimitives.panel(guiGraphics, px, py, WIDTH, HEIGHT);

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, TEXT_TITLE,
                px + WIDTH / 2, py + 6, TEXT_COLOR);
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, TEXT_HINT, px + 8, py + 18, WIDTH - 16, 0xFFE6D6A8);
        guiGraphics.drawString(Minecraft.getInstance().font, TEXT_NAME, px + 8, py + 46, 0xFFE0B86A, false);

        int fieldX = px + 8;
        int fieldY = py + 58;
        int fieldW = WIDTH - 16;
        guiGraphics.fill(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + 15, 0x80303030);
        guiGraphics.renderOutline(fieldX - 1, fieldY - 1, fieldW + 2, 16, 0xAA8A6A3A);

        if (nameField != null) nameField.render(guiGraphics, mouseX, mouseY, 0);

        Component status = canConfirm() ? TEXT_HINT : TEXT_DISABLED_BLANK;
        int statusColor = canConfirm() ? 0xFFB8A17A : 0xFFFFD36A;
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, status, px + 8, py + 78, WIDTH - 16, statusColor);

        int btnY = py + HEIGHT - 18;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_CONFIRM, px + 8, btnY, 90, 14, canConfirm());
        renderButton(guiGraphics, mouseX, mouseY, TEXT_CANCEL, px + WIDTH - 98, btnY, 90, 14, true);
    }

    private void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                               Component label, int x, int y, int w, int h, boolean enabled) {
        WorldMapRenderPrimitives.button(guiGraphics, Minecraft.getInstance().font, mouseX, mouseY,
                x, y, w, h, label, TEXT_COLOR, false, enabled);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible) return false;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;

        // Click outside — close
        if (mouseX < px || mouseX > px + WIDTH || mouseY < py || mouseY > py + HEIGHT) {
            close();
            return true;
        }

        // Forward to EditBox so cursor repositions on click
        if (nameField != null) nameField.mouseClicked(mouseX, mouseY, 0);

        int btnY = py + HEIGHT - 18;

        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + 8, btnY, 90, 14)) {
            confirm();
            return true;
        }
        if (WorldMapRenderPrimitives.contains(mouseX, mouseY, px + WIDTH - 98, btnY, 90, 14)) {
            close();
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        // Forward all other keys to EditBox (arrows, home, end, backspace, delete, ctrl+a, etc.)
        if (nameField != null) nameField.keyPressed(keyCode, 0, 0);
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible) return false;
        if (nameField != null) nameField.charTyped(chr, modifiers);
        return true;
    }
}
