package com.talhanation.recruits.client.gui.worldmap.ui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyEditScreen;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.client.gui.worldmap.claim.ClaimEditScreen;
import com.talhanation.recruits.client.gui.worldmap.claim.WorldMapClaimIndex;
import com.talhanation.recruits.network.MessageTeleportPlayer;
import com.talhanation.recruits.network.MessageUpdateClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorldMapContextMenu {

    private static final Component TEXT_DIPLOMACY = Component.translatable("gui.recruits.map.diplomacy");
    private static final Component TEXT_CLAIM_CHUNK = Component.translatable("gui.recruits.map.claim_chunk");
    private static final Component TEXT_CLAIM_AREA = Component.translatable("gui.recruits.map.claim_area");
    private static final Component TEXT_EDIT_CLAIM = Component.translatable("gui.recruits.map.edit_claim");
    private static final Component TEXT_REMOVE_CHUNK = Component.translatable("gui.recruits.map.remove_chunk");
    private static final Component TEXT_CENTER_MAP = Component.translatable("gui.recruits.map.center_map");
    private static final Component TEXT_REMOVE_CHUNK_ADMIN =
            Component.translatable("gui.recruits.map.remove_chunk_admin");
    private static final Component TEXT_DELETE_CLAIM_ADMIN =
            Component.translatable("gui.recruits.map.delete_claim_admin");
    private static final Component TEXT_TELEPORT_ADMIN = Component.translatable("gui.recruits.map.teleport_admin");

    private final List<ContextMenuEntry> entries = new ArrayList<>();
    private final List<ContextMenuEntry> visibleEntries = new ArrayList<>();
    private final List<Boolean> visibleEntryEnabled = new ArrayList<>();
    private int x, y;
    private boolean visible = false;
    private final int width = 150;
    private final int entryHeight = 20;
    private final WorldMapScreen worldMapScreen;

    /**
     * Snapshot of mouse position taken when the menu was opened. Used for
     * waypoint hit-detection.
     */
    private double snapshotMouseX;
    private double snapshotMouseY;

    public WorldMapContextMenu(WorldMapScreen worldMapScreen) {
        this.worldMapScreen = worldMapScreen;
        ItemStack itemStackClaimChunk = new ItemStack(ClientManager.currencyItemStack.getItem());
        itemStackClaimChunk.setCount(ClientManager.configValueChunkCost);
        ItemStack itemStackClaimArea = new ItemStack(ClientManager.currencyItemStack.getItem());
        itemStackClaimArea.setCount(worldMapScreen.getClaimCost(ClientManager.ownFaction));

        addEntry(TEXT_DIPLOMACY.getString(),
                () -> ClientManager.ownFaction != null
                        && this.worldMapScreen.selectedClaim() != null
                        && this.worldMapScreen.selectedClaim().getOwnerFaction() != null
                        && !ClientManager.ownFaction.getStringID()
                                .equals(this.worldMapScreen.selectedClaim().getOwnerFactionStringID()),
                (screen) -> Minecraft.getInstance()
                        .setScreen(new DiplomacyEditScreen(screen, screen.selectedClaim().getOwnerFaction()))
        );

        addEntry(TEXT_CLAIM_CHUNK.getString(),
                this.worldMapScreen::canShowClaimChunkEntry,
                this.worldMapScreen::canExecuteClaimChunkEntry,
                WorldMapScreen::claimChunk,
                itemStackClaimChunk,
                "bufferzone, chunk",
                this.worldMapScreen::getClaimChunkDisabledReason
        );

        addEntry(TEXT_CLAIM_AREA.getString(),
                this.worldMapScreen::canShowClaimAreaEntry,
                this.worldMapScreen::canExecuteClaimAreaEntry,
                WorldMapScreen::claimArea,
                itemStackClaimArea,
                "bufferzone, area",
                this.worldMapScreen::getClaimAreaDisabledReason
        );

        addEntry("Add Waypoint",
                () -> this.worldMapScreen.selectedRoute != null
                        && this.worldMapScreen.canPlaceWaypointAt(
                                this.worldMapScreen.snapshotWorldX(),
                                this.worldMapScreen.snapshotWorldZ()),
                WorldMapScreen::addWaypointAtClicked
        );

        addEntry("Remove Waypoint",
                () -> this.worldMapScreen.selectedRoute != null
                        && this.worldMapScreen.isWaypointHoveredAt(snapshotMouseX, snapshotMouseY),
                (screen) -> screen.removeWaypointAt(snapshotMouseX, snapshotMouseY)
        );

        addEntry("Edit Waypoint",
                () -> this.worldMapScreen.selectedRoute != null
                        && this.worldMapScreen.isWaypointHoveredAt(snapshotMouseX, snapshotMouseY),
                (screen) -> screen.openWaypointEditPopup(snapshotMouseX, snapshotMouseY)
        );

        addEntry(TEXT_EDIT_CLAIM.getString(),
                () -> (this.worldMapScreen.selectedClaim() != null
                        && this.worldMapScreen.isPlayerFactionLeader(
                                this.worldMapScreen.selectedClaim().getOwnerFaction()))
                        || this.worldMapScreen.isPlayerClaimLeader(),
                (screen) -> {
                    screen.getMinecraft()
                            .setScreen(new ClaimEditScreen(screen, screen.selectedClaim(), screen.getPlayer()));
                    screen.clearSelectedClaim();
                }
        );

        addEntry(TEXT_REMOVE_CHUNK.getString(),
                () -> this.worldMapScreen.selectedChunk() != null
                        && this.worldMapScreen.selectedClaim() != null
                        && worldMapScreen.canRemoveChunk(worldMapScreen.selectedChunk(), worldMapScreen.selectedClaim())
                        && (this.worldMapScreen.isPlayerFactionLeader(worldMapScreen.selectedClaim().getOwnerFaction())
                                || this.worldMapScreen.isPlayerClaimLeader(worldMapScreen.selectedClaim())),
                (screen) -> {
                    if (screen.selectedClaim().containsChunk(screen.selectedChunk())) {
                        screen.selectedClaim().removeChunk(screen.selectedChunk());
                        WorldMapClaimIndex.invalidate();
                        screen.clearSelectedChunk();
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim()));
                    }
                }
        );

        addEntry(TEXT_CENTER_MAP.getString(), WorldMapScreen::centerOnPlayer);

        addEntry(TEXT_REMOVE_CHUNK_ADMIN.getString(),
                () -> this.worldMapScreen.selectedChunk() != null
                        && this.worldMapScreen.selectedClaim() != null
                        && this.worldMapScreen.isPlayerAdminAndCreative()
                        && worldMapScreen.canRemoveChunk(worldMapScreen.selectedChunk(), worldMapScreen.selectedClaim())
                        && !(this.worldMapScreen.isPlayerFactionLeader(worldMapScreen.selectedClaim().getOwnerFaction())
                                || this.worldMapScreen.isPlayerClaimLeader(worldMapScreen.selectedClaim())),
                (screen) -> {
                    if (screen.selectedClaim().containsChunk(screen.selectedChunk())) {
                        screen.selectedClaim().removeChunk(screen.selectedChunk());
                        WorldMapClaimIndex.invalidate();
                        screen.clearSelectedChunk();
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim()));
                    }
                },
                "admin"
        );

        addEntry(TEXT_DELETE_CLAIM_ADMIN.getString(),
                () -> this.worldMapScreen.isPlayerAdminAndCreative() && this.worldMapScreen.selectedClaim() != null,
                (screen) -> {
                    screen.selectedClaim().isRemoved = true;
                    WorldMapClaimIndex.invalidate();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim()));
                    screen.clearSelectedClaim();
                },
                "admin"
        );

        addEntry(TEXT_TELEPORT_ADMIN.getString(),
                worldMapScreen::isPlayerAdminAndCreative,
                (screen) -> Main.SIMPLE_CHANNEL.sendToServer(new MessageTeleportPlayer(screen.getClickedBlockPos())),
                "admin"
        );
    }

    public void addEntry(
            String text,
            BooleanSupplier condition,
            Consumer<WorldMapScreen> action,
            ItemStack itemStack,
            String tag) {
        addEntry(text, condition, () -> true, action, itemStack, tag, Component::empty);
    }

    public void addEntry(
            String text,
            BooleanSupplier visibleCondition,
            BooleanSupplier enabledCondition,
            Consumer<WorldMapScreen> action,
            ItemStack itemStack,
            String tag,
            Supplier<Component> disabledReason) {
        entries.add(new ContextMenuEntry(
                Component.literal(text),
                visibleCondition,
                enabledCondition,
                action,
                itemStack,
                tag,
                disabledReason));
    }

    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action, String tag) {
        addEntry(text, condition, () -> true, action, ItemStack.EMPTY, tag, Component::empty);
    }

    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action) {
        addEntry(text, condition, () -> true, action, ItemStack.EMPTY, "", Component::empty);
    }

    public void addEntry(String text, Consumer<WorldMapScreen> action) {
        addEntry(text, () -> true, action);
    }

    public void openAt(int x, int y) {
        // Snapshot mouse position at open time for waypoint hit-detection.
        this.snapshotMouseX = x;
        this.snapshotMouseY = y;
        rebuildVisibleEntries(worldMapScreen);

        int visibleEntryCount = Math.max(1, visibleEntries.size());
        this.x = Math.max(10, Math.min(x, worldMapScreen.width - width - 10));
        this.y = Math.max(10, Math.min(y, worldMapScreen.height - visibleEntryCount * entryHeight - 10));
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.hoveredEntryTag = null;
        this.visibleEntries.clear();
        this.visibleEntryEnabled.clear();
    }
    public boolean isVisible() { return visible; }

    private String hoveredEntryTag = null;

    public String getHoveredEntryTag() { return hoveredEntryTag; }

    public void render(GuiGraphics guiGraphics, WorldMapScreen screen) {
        if (!visible) return;

        hoveredEntryTag = null;

        int height = visibleEntries.size() * entryHeight;

        guiGraphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        guiGraphics.renderOutline(x, y, width, height, 0xFF555555);

        int entryY = y;
        ContextMenuEntry hoveredDisabledEntry = null;
        for (int i = 0; i < visibleEntries.size(); i++) {
            ContextMenuEntry entry = visibleEntries.get(i);
            boolean hovered = isMouseOverEntry(x, entryY);
            boolean enabled = visibleEntryEnabled.get(i);
            if (hovered) hoveredEntryTag = entry.getTag();
            if (hovered && !enabled) hoveredDisabledEntry = entry;

            guiGraphics.fill(
                    x, entryY, x + width, entryY + entryHeight, hovered ? 0xFF333333 : 0xFF1A1A1A);

            int textColor;
            if (!enabled) {
                textColor = 0xFF777777;
            } else if (entry.getTag().equals("admin")) {
                textColor = hovered ? 0xFFFF5555 : 0xFFAA4444;
            } else {
                textColor = hovered ? 0xFFFFFF : 0xCCCCCC;
            }

            guiGraphics.drawString(screen.getMinecraft().font, entry.text(), x + 8, entryY + 6, textColor);

            if (!entry.stack.isEmpty()) {
                guiGraphics.renderFakeItem(entry.stack, x + width - 20, entryY + 1);
                guiGraphics.renderItemDecorations(
                        screen.getMinecraft().font, entry.stack, x + width - 20, entryY + 1);
            }

            entryY += entryHeight;
        }

        if (hoveredDisabledEntry != null) {
            renderDisabledReason(guiGraphics, screen, hoveredDisabledEntry.getDisabledReason());
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, WorldMapScreen screen) {
        if (!visible || button != 0) return false;

        int entryY = y;
        for (ContextMenuEntry entry : visibleEntries) {
            if (mouseX >= x
                    && mouseX <= x + width
                    && mouseY >= entryY
                    && mouseY <= entryY + entryHeight) {
                if (!entry.isEnabled(screen)) {
                    return true;
                }
                entry.execute(screen);
                close();
                return true;
            }
            entryY += entryHeight;
        }

        close();
        return false;
    }

    private void rebuildVisibleEntries(WorldMapScreen screen) {
        visibleEntries.clear();
        visibleEntryEnabled.clear();
        for (ContextMenuEntry entry : entries) {
            if (entry.shouldShow(screen)) {
                visibleEntries.add(entry);
                visibleEntryEnabled.add(entry.isEnabled(screen));
            }
        }
    }

    private boolean isMouseOverEntry(int entryX, int entryY) {
        double mouseX = this.worldMapScreen.mouseX;
        double mouseY = this.worldMapScreen.mouseY;
        return mouseX >= entryX
                && mouseX <= entryX + width
                && mouseY >= entryY
                && mouseY <= entryY + entryHeight;
    }

    private void renderDisabledReason(GuiGraphics guiGraphics, WorldMapScreen screen, Component reason) {
        if (reason == null || reason.getString().isBlank()) return;

        int padding = 5;
        int textWidth = screen.getMinecraft().font.width(reason);
        int tooltipWidth = textWidth + padding * 2;
        int tooltipHeight = 18;
        int tooltipX = x + width + 6;
        if (tooltipX + tooltipWidth > screen.width - 6) {
            tooltipX = Math.max(6, x - tooltipWidth - 6);
        }
        int tooltipY = Math.max(6, Math.min((int) worldMapScreen.mouseY - 8, screen.height - tooltipHeight - 6));

        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xDD101010);
        guiGraphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xFF555555);
        guiGraphics.drawString(screen.getMinecraft().font, reason, tooltipX + padding, tooltipY + 5, 0xFFE0E0E0);
    }

    private record ContextMenuEntry(
            Component text,
            BooleanSupplier visibleCondition,
            BooleanSupplier enabledCondition,
            Consumer<WorldMapScreen> action,
            ItemStack stack,
            String tag,
            Supplier<Component> disabledReasonSupplier) {
        public String getTag() { return tag == null ? "" : tag; }
        public boolean shouldShow(WorldMapScreen screen) { return visibleCondition.getAsBoolean(); }
        public boolean isEnabled(WorldMapScreen screen) { return enabledCondition.getAsBoolean(); }
        public void execute(WorldMapScreen screen) { action.accept(screen); }
        public Component getDisabledReason() {
            return disabledReasonSupplier == null ? Component.empty() : disabledReasonSupplier.get();
        }
    }
}
