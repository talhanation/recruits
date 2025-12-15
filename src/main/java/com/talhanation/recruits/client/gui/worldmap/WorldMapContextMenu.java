package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyEditScreen;
import com.talhanation.recruits.network.MessageTeleportPlayer;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.Minecraft;
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
    private final int width = 150;
    private final int entryHeight = 20;
    private final WorldMapScreen worldMapScreen;

    public WorldMapContextMenu(WorldMapScreen worldMapScreen) {
        this.worldMapScreen = worldMapScreen;

        addEntry("Diplomacy",
                () -> ClientManager.ownFaction != null && !this.worldMapScreen.isPlayerFactionLeader() && !this.worldMapScreen.isPlayerClaimLeader(),
                (screen) -> Minecraft.getInstance().setScreen(new DiplomacyEditScreen(screen, screen.selectedClaim.getOwnerFaction()))
        );

        addEntry("Claim Chunk",
                () -> (this.worldMapScreen.isPlayerFactionLeader() || this.worldMapScreen.isPlayerClaimLeader()),

                WorldMapScreen::claimChunk
        );

        addEntry("Claim Area",
                worldMapScreen::isPlayerFactionLeader,
                WorldMapScreen::claimArea
        );

        addEntry("Edit Claim",
                () -> (this.worldMapScreen.isPlayerFactionLeader() || this.worldMapScreen.isPlayerClaimLeader()),

                (screen) -> Minecraft.getInstance().setScreen(new ClaimEditScreen(screen, screen.selectedClaim, screen.getPlayer()))
        );

        addEntry("Center Map", WorldMapScreen::centerOnPlayer);


        //ADMIN STUFF


        addEntry("Teleport (Admin)",
                worldMapScreen::isPlayerAdmin,
                screen -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageTeleportPlayer(screen.getClickedBlockPos()));
                });
    }
    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action) {
        entries.add(new ContextMenuEntry(Component.literal(text), condition, action));
    }

    public void addEntry(String text, Consumer<WorldMapScreen> action) {
        addEntry(text, () -> true, action);
    }

    public void openAt(int x, int y) {
        this.x = Math.max(10, Math.min(x, worldMapScreen.width - width - 10));
        this.y = Math.max(10, Math.min(y, worldMapScreen.height - entries.size() * entryHeight - 10));
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

        int visibleEntries = (int) entries.stream()
                .filter(entry -> entry.shouldShow(screen))
                .count();
        int height = visibleEntries * entryHeight;

        guiGraphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        guiGraphics.renderOutline(x, y, width, height, 0xFF555555);

        int entryY = y;
        for (ContextMenuEntry entry : entries) {
            if (entry.shouldShow(screen)) {
                boolean hovered = isMouseOverEntry(x, entryY);
                int bgColor = hovered ? 0xFF333333 : 0xFF1A1A1A;
                guiGraphics.fill(x, entryY, x + width, entryY + entryHeight, bgColor);

                int textColor;
                if (entry.text().getString().contains("(Admin)")) {
                    textColor = hovered ? 0xFFFF5555 : 0xFFAA4444;
                } else {
                    textColor = hovered ? 0xFFFFFF : 0xCCCCCC;
                }

                guiGraphics.drawString(screen.getMinecraft().font, entry.text(), x + 8, entryY + 6, textColor);
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

    private boolean isMouseOverEntry(int entryX, int entryY) {
        double mouseX = this.worldMapScreen.mouseX;
        double mouseY = this.worldMapScreen.mouseY;
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