package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.network.MessageTransferRoute;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class RouteEditPopup {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 190;
    private static final int BTN_COLOR = 0x80222222;
    private static final int BTN_HOVERED_COLOR = 0x80444444;
    private static final int BTN_DELETE_COLOR = 0x80330000;
    private static final int BTN_DELETE_HOVERED = 0x80660000;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int BTN_H = 16;
    private static final int BTN_W_FULL = WIDTH - 16;
    private static final Component TEXT_TITLE = Component.translatable("gui.recruits.map.route.edit.title");
    private static final Component TEXT_HINT = Component.translatable("gui.recruits.map.route.edit.hint");
    private static final Component TEXT_TRANSFER_TITLE = Component.translatable("gui.recruits.map.route.edit.transfer.title");
    private static final Component TEXT_TRANSFER_ACTION = Component.translatable("gui.recruits.map.route.edit.transfer.action");
    private static final Component TEXT_TRANSFER_HINT = Component.translatable("gui.recruits.map.route.edit.transfer.hint");
    private static final Component TEXT_NAME = Component.translatable("gui.recruits.map.route.name");
    private static final Component TEXT_SAVE = Component.translatable("gui.recruits.map.route.edit.save");
    private static final Component TEXT_TRANSFER = Component.translatable("gui.recruits.map.route.edit.transfer");
    private static final Component TEXT_DELETE = Component.translatable("gui.recruits.map.route.edit.delete");
    private static final Component TEXT_BACK = Component.translatable("gui.recruits.map.route.edit.back");
    private static final Component TEXT_DISABLED_BLANK = Component.translatable("gui.recruits.map.route.disabled.blank_name");
    private static final Component TEXT_DISABLED_SAME = Component.translatable("gui.recruits.map.route.disabled.same_name");
    private static final Component TEXT_FEEDBACK_SAVED = Component.translatable("gui.recruits.map.route.feedback.saved");
    private static final Component TEXT_FEEDBACK_TRANSFER = Component.translatable("gui.recruits.map.route.feedback.transfer");
    private static final Component TEXT_FEEDBACK_DELETED = Component.translatable("gui.recruits.map.route.feedback.deleted");

    private final WorldMapScreen parent;
    private final Player player;

    private boolean visible = false;
    private RecruitsRoute route;
    private EditBox nameField;

    public RouteEditPopup(WorldMapScreen parent, Player player) {
        this.parent = parent;
        this.player = player;
    }

    public boolean isVisible() {
        return visible;
    }

    public void open(RecruitsRoute route) {
        this.route = route;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;
        int fieldX = px + 8;
        int fieldY = py + 58;
        int fieldW = WIDTH - 16;

        nameField = new EditBox(Minecraft.getInstance().font,
                fieldX, fieldY + 3, fieldW, 8, Component.empty());
        nameField.setMaxLength(32);
        nameField.setValue(route.getName());
        nameField.setFocused(true);
        nameField.setBordered(false);
        nameField.setTextColor(TEXT_COLOR);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.route = null;
        this.nameField = null;
    }

    private void save() {
        if (!canSave()) return;

        String trimmed = nameField.getValue().trim();
        ClientManager.renameRoute(route, trimmed);

        parent.refreshRouteUI();
        parent.showMapNotice(TEXT_FEEDBACK_SAVED, 0xFF9FDB6B);
        close();
    }

    private void openTransfer() {
        RecruitsRoute transferRoute = route;
        close();
        Minecraft.getInstance().setScreen(new SelectPlayerScreen(
                parent, player,
                TEXT_TRANSFER_TITLE,
                TEXT_TRANSFER_ACTION,
                TEXT_TRANSFER_HINT,
                false,
                PlayersList.FilterType.NONE,
                (playerInfo) -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageTransferRoute(playerInfo.getUUID(), transferRoute));
                    parent.showMapNotice(TEXT_FEEDBACK_TRANSFER, 0xFFFFD36A);
                    Minecraft.getInstance().setScreen(parent);
                }
        ));
    }

    private void deleteRoute() {
        ClientManager.deleteRoute(route);
        parent.setSelectedRoute(null);
        parent.refreshRouteUI();
        parent.showMapNotice(TEXT_FEEDBACK_DELETED, 0xFFFF8A7A);
        close();
    }

    private boolean canSave() {
        return nameField != null && route != null && !nameField.getValue().trim().isBlank() && !nameField.getValue().trim().equals(route.getName());
    }

    private Component getSaveHint() {
        if (nameField == null || route == null) {
            return TEXT_HINT;
        }
        String trimmed = nameField.getValue().trim();
        if (trimmed.isBlank()) {
            return TEXT_DISABLED_BLANK;
        }
        if (trimmed.equals(route.getName())) {
            return TEXT_DISABLED_SAME;
        }
        return TEXT_HINT;
    }

    public void tick() {
        if (visible && nameField != null) nameField.tick();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!visible) return;

        int px = (parent.width - WIDTH) / 2;
        int py = (parent.height - HEIGHT) / 2;
        int x = px + 8;

        guiGraphics.fill(0, 0, parent.width, parent.height, 0x88000000);
        WorldMapRenderPrimitives.panel(guiGraphics, px, py, WIDTH, HEIGHT);

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, TEXT_TITLE,
                px + WIDTH / 2, py + 6, TEXT_COLOR);
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, TEXT_HINT, x, py + 18, BTN_W_FULL, 0xFFE6D6A8);
        guiGraphics.drawString(Minecraft.getInstance().font, TEXT_NAME, x, py + 46, 0xFFE0B86A, false);

        // Name field
        int fieldY = py + 58;
        guiGraphics.fill(x - 1, fieldY - 1, x + BTN_W_FULL + 1, fieldY + 15, 0x80303030);
        guiGraphics.renderOutline(x - 1, fieldY - 1, BTN_W_FULL + 2, 16, 0xAA8A6A3A);
        if (nameField != null) nameField.render(guiGraphics, mouseX, mouseY, 0);

        guiGraphics.drawWordWrap(Minecraft.getInstance().font, getSaveHint(), x, fieldY + 18, BTN_W_FULL, canSave() ? 0xFFB8A17A : 0xFFFFD36A);

        int y = fieldY + 46;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_SAVE,     x, y, BTN_W_FULL, BTN_H, false, canSave()); y += BTN_H + 4;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_TRANSFER, x, y, BTN_W_FULL, BTN_H, false, true); y += BTN_H + 4;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_DELETE,   x, y, BTN_W_FULL, BTN_H, true, true);  y += BTN_H + 4;
        renderButton(guiGraphics, mouseX, mouseY, TEXT_BACK,     x, y, BTN_W_FULL, BTN_H, false, true);
    }

    private void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                               Component label, int x, int y, int w, int h, boolean red, boolean enabled) {
        if (red) {
            boolean hovered = enabled && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            int bg = !enabled ? 0x66251212 : (hovered ? BTN_DELETE_HOVERED : BTN_DELETE_COLOR);
            guiGraphics.fill(x, y, x + w, y + h, bg);
            guiGraphics.renderOutline(x, y, w, h, !enabled ? 0x665E4A2A : 0xFFB85A52);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, label,
                    x + w / 2, y + (h - 8) / 2, enabled ? TEXT_COLOR : 0xFF9A8661);
            return;
        }
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

        // Forward to EditBox for cursor repositioning
        if (nameField != null) nameField.mouseClicked(mouseX, mouseY, 0);

        int x = px + 8;
        int y = py + 58 + 46; // after hint, name field, and save hint

        if (isOver(mouseX, mouseY, x, y, BTN_W_FULL, BTN_H)) { save();         return true; } y += BTN_H + 4;
        if (isOver(mouseX, mouseY, x, y, BTN_W_FULL, BTN_H)) { openTransfer(); return true; } y += BTN_H + 4;
        if (isOver(mouseX, mouseY, x, y, BTN_W_FULL, BTN_H)) { deleteRoute();  return true; } y += BTN_H + 4;
        if (isOver(mouseX, mouseY, x, y, BTN_W_FULL, BTN_H)) { close();        return true; }

        return true;
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public boolean keyPressed(int keyCode) {
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (nameField != null) nameField.keyPressed(keyCode, 0, 0);
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible) return false;
        if (nameField != null) nameField.charTyped(chr, modifiers);
        return true;
    }
}
