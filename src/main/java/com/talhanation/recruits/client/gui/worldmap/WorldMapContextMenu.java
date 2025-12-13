package com.talhanation.recruits.client.gui.worldmap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class WorldMapContextMenu {
    private final List<ContextMenuEntry> entries = new ArrayList<>();
    private int x, y;
    private boolean visible = false;
    private final int width = 120;
    private final int entryHeight = 20;
    private final WorldMapScreen mapScreen;
    public WorldMapContextMenu(WorldMapScreen mapScreen) {
        this.mapScreen = mapScreen;
        addEntry("Chunk auswählen",
                () -> true,
                screen -> {
                    if (screen.getHoveredChunk() != null) {
                        screen.setSelectedChunk(screen.getHoveredChunk());
                    }
                });

        addEntry("Auswahl löschen",
                () -> true,
                screen -> screen.setSelectedChunk(null));

        addEntry("Zum Spieler zentrieren",
                () -> true,
                screen -> screen.centerOnPlayer());

        addEntry("Zoom zurücksetzen",
                () -> true,
                screen -> screen.resetZoom());
    }

    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action) {
        entries.add(new ContextMenuEntry(Component.literal(text), condition, action));
    }

    public void openAt(int x, int y, WorldMapScreen screen) {
        this.x = Math.max(0, Math.min(x, screen.width - width));
        this.y = Math.max(0, Math.min(y, screen.height - entries.size() * entryHeight));
        this.visible = true;
    }

    public void close() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(GuiGraphics guiGraphics, WorldMapScreen screen) {
        if (!visible) return;

        guiGraphics.fill(x, y, x + width, y + entries.size() * entryHeight, 0xFF2C2C2C);
        guiGraphics.renderOutline(x, y, width, entries.size() * entryHeight, 0xFF555555);

        int entryY = y;
        for (ContextMenuEntry entry : entries) {
            if (entry.shouldShow(screen)) {
                int color = isMouseOverEntry((int) this.mapScreen.mouseX, (int) this.mapScreen.mouseY, x, entryY) ? 0xFFDDDDDD : 0xFFAAAAAA;
                guiGraphics.drawString(screen.getMinecraft().font, entry.text(), x + 5, entryY + 6, color);
                entryY += entryHeight;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, WorldMapScreen screen) {
        if (!visible || button != 0) return false;

        int entryY = y;
        for (ContextMenuEntry entry : entries) {
            if (entry.shouldShow(screen)) {
                if (mouseX >= x && mouseX <= x + width &&
                        mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    entry.execute(screen);
                    close();
                    return true;
                }
                entryY += entryHeight;
            }
        }

        close();
        return false;
    }

    private boolean isMouseOverEntry(int mouseX, int mouseY, int entryX, int entryY) {
        return mouseX >= entryX && mouseX <= entryX + width &&
                mouseY >= entryY && mouseY <= entryY + entryHeight;
    }

    private record ContextMenuEntry(
            Component text,
            BooleanSupplier condition,
            Consumer<WorldMapScreen> action
    ) {
        public boolean shouldShow(WorldMapScreen screen) {
            return condition.getAsBoolean();
        }

        public void execute(WorldMapScreen screen) {
            action.accept(screen);
        }
    }
}
