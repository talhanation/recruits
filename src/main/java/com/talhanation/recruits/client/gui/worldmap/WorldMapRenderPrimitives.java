package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class WorldMapRenderPrimitives {
    private WorldMapRenderPrimitives() {
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xB8201810);
        graphics.renderOutline(x, y, width, height, 0xFF8A6A3A);
        graphics.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x66301810);
    }

    static void button(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                       int x, int y, int width, int height, String label,
                       int labelColor, boolean selected, boolean enabled) {
        button(graphics, font, mouseX, mouseY, x, y, width, height, Component.literal(label), labelColor, selected, enabled);
    }

    static void button(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                       int x, int y, int width, int height, Component label,
                       int labelColor, boolean selected, boolean enabled) {
        boolean hovered = contains(mouseX, mouseY, x, y, width, height);
        int bg = !enabled ? 0x88221812 : selected ? 0xCC5A4025 : (hovered ? 0xCC4B3928 : 0xB82A2119);
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.renderOutline(x, y, width, height, !enabled ? 0x665E4A2A : hovered || selected ? 0xFFE0B86A : 0xAA8A6A3A);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2, enabled ? labelColor : 0xFF9A8661);
    }

    static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
