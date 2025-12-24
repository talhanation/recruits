package com.talhanation.recruits.client.gui.worldmap;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.diplomacy.DiplomacyEditScreen;
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

public class WorldMapContextMenu {

    private static final Component TEXT_DIPLOMACY = Component.translatable("gui.recruits.map.diplomacy");
    private static final Component TEXT_CLAIM_CHUNK = Component.translatable("gui.recruits.map.claim_chunk");
    private static final Component TEXT_CLAIM_AREA = Component.translatable("gui.recruits.map.claim_area");
    private static final Component TEXT_EDIT_CLAIM = Component.translatable("gui.recruits.map.edit_claim");
    private static final Component TEXT_REMOVE_CHUNK = Component.translatable("gui.recruits.map.remove_chunk");
    private static final Component TEXT_CENTER_MAP = Component.translatable("gui.recruits.map.center_map");

    private static final Component TEXT_REMOVE_CHUNK_ADMIN = Component.translatable("gui.recruits.map.remove_chunk_admin");
    private static final Component TEXT_DELETE_CLAIM_ADMIN = Component.translatable("gui.recruits.map.delete_claim_admin");
    private static final Component TEXT_TELEPORT_ADMIN = Component.translatable("gui.recruits.map.teleport_admin");
    private final List<ContextMenuEntry> entries = new ArrayList<>();
    private int x, y;
    private boolean visible = false;
    private final int width = 150;
    private final int entryHeight = 20;
    private final WorldMapScreen worldMapScreen;

    public WorldMapContextMenu(WorldMapScreen worldMapScreen) {
        this.worldMapScreen = worldMapScreen;
        ItemStack claimChunk = new ItemStack(ClientManager.currencyItemStack.getItem());
        claimChunk.setCount(ClientManager.configValueChunkCost);
        ItemStack claimArea = new ItemStack(ClientManager.currencyItemStack.getItem());
        claimArea.setCount(ClientManager.configValueClaimCost);

        addEntry(TEXT_DIPLOMACY.getString(),
                () -> (ClientManager.ownFaction != null
                        && this.worldMapScreen.selectedClaim != null && this.worldMapScreen.selectedClaim.getOwnerFaction() != null
                        && !ClientManager.ownFaction.getStringID().equals(this.worldMapScreen.selectedClaim.getOwnerFactionStringID())
                ),
                (screen) -> Minecraft.getInstance().setScreen(new DiplomacyEditScreen(screen, screen.selectedClaim.getOwnerFaction()))
        );

        addEntry(TEXT_CLAIM_CHUNK.getString(),
                () -> (this.worldMapScreen.canClaimChunk(worldMapScreen.selectedChunk)
                        && (this.worldMapScreen.isPlayerFactionLeader() || this.worldMapScreen.isPlayerClaimLeader(worldMapScreen.getNeighborClaim(worldMapScreen.selectedChunk)))
                ),
                WorldMapScreen::claimChunk,
                claimChunk,
                "bufferzone, chunk"
        );

        addEntry(TEXT_CLAIM_AREA.getString(),
                () -> (this.worldMapScreen.canClaimArea(worldMapScreen.getClaimArea(worldMapScreen.selectedChunk))
                        && (this.worldMapScreen.isPlayerFactionLeader())
                ),
                WorldMapScreen::claimArea,
                claimArea,
                "bufferzone, area"
        );

        addEntry(TEXT_EDIT_CLAIM.getString(),
                () -> (this.worldMapScreen.selectedClaim != null
                        && this.worldMapScreen.isPlayerFactionLeader(this.worldMapScreen.selectedClaim.getOwnerFaction())
                        || this.worldMapScreen.isPlayerClaimLeader()
                ),
                (screen) ->{
                    screen.getMinecraft().setScreen(new ClaimEditScreen(screen, screen.selectedClaim, screen.getPlayer()));
                    screen.selectedClaim = null;
                }
        );

        addEntry(TEXT_REMOVE_CHUNK.getString(),
                () -> (this.worldMapScreen.selectedChunk != null && this.worldMapScreen.selectedClaim != null
                        && worldMapScreen.canRemoveChunk(worldMapScreen.selectedChunk, worldMapScreen.selectedClaim)
                        && (this.worldMapScreen.isPlayerFactionLeader(worldMapScreen.selectedClaim.getOwnerFaction()) || this.worldMapScreen.isPlayerClaimLeader(worldMapScreen.selectedClaim))
                ),

                (screen) ->{
                    if(screen.selectedClaim.containsChunk(screen.selectedChunk)){
                        screen.selectedClaim.removeChunk(screen.selectedChunk);
                        screen.selectedChunk = null;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim));
                    }
                }
        );

        addEntry(TEXT_CENTER_MAP.getString(), WorldMapScreen::centerOnPlayer);

        //ADMIN STUFF
        addEntry(TEXT_REMOVE_CHUNK_ADMIN.getString(),
                () -> (this.worldMapScreen.selectedChunk != null && this.worldMapScreen.selectedClaim != null
                        && this.worldMapScreen.isPlayerAdminAndCreative()
                        && worldMapScreen.canRemoveChunk(worldMapScreen.selectedChunk, worldMapScreen.selectedClaim)
                        && !(this.worldMapScreen.isPlayerFactionLeader(worldMapScreen.selectedClaim.getOwnerFaction()) || this.worldMapScreen.isPlayerClaimLeader(worldMapScreen.selectedClaim))
                ),
                (screen) ->{
                    if(screen.selectedClaim.containsChunk(screen.selectedChunk)){
                        screen.selectedClaim.removeChunk(screen.selectedChunk);
                        screen.selectedChunk = null;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim));
                    }
                },
                "admin"
        );

        addEntry(TEXT_DELETE_CLAIM_ADMIN.getString(),
                () -> (this.worldMapScreen.isPlayerAdminAndCreative() && this.worldMapScreen.selectedClaim != null),
                screen -> {
                    screen.selectedClaim.isRemoved = true;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(screen.selectedClaim));
                    screen.selectedClaim = null;
                },
                "admin"
                );

        addEntry(TEXT_TELEPORT_ADMIN.getString(),
                worldMapScreen::isPlayerAdminAndCreative,
                screen -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageTeleportPlayer(screen.getClickedBlockPos()));
                },
                "admin"
                );
    }
    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action, ItemStack itemStack, String tag) {
        entries.add(new ContextMenuEntry(Component.literal(text), condition, action, itemStack, tag));
    }
    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action, String tag) {
        entries.add(new ContextMenuEntry(Component.literal(text), condition, action, ItemStack.EMPTY, tag));
    }
    public void addEntry(String text, BooleanSupplier condition, Consumer<WorldMapScreen> action) {
        entries.add(new ContextMenuEntry(Component.literal(text), condition, action, ItemStack.EMPTY, ""));
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

    private String hoveredEntryTag = null;

    public String getHoveredEntryTag() {
        return hoveredEntryTag;
    }

    public void render(GuiGraphics guiGraphics, WorldMapScreen screen) {
        if (!visible) return;

        hoveredEntryTag = null;

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

                // Set hovered entry text
                if (hovered) {
                    hoveredEntryTag = entry.getTag();
                }

                int bgColor = hovered ? 0xFF333333 : 0xFF1A1A1A;
                guiGraphics.fill(x, entryY, x + width, entryY + entryHeight, bgColor);

                int textColor;
                if (entry.getTag().equals("admin")) {
                    textColor = hovered ? 0xFFFF5555 : 0xFFAA4444;
                } else {
                    textColor = hovered ? 0xFFFFFF : 0xCCCCCC;
                }

                guiGraphics.drawString(screen.getMinecraft().font, entry.text(), x + 8, entryY + 6, textColor);

                if(!entry.stack.isEmpty()){
                    guiGraphics.renderFakeItem(entry.stack, x + width - 20, entryY + 1);
                    guiGraphics.renderItemDecorations(screen.getMinecraft().font, entry.stack, x + width - 20, entryY + 1);
                }

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

    private record ContextMenuEntry (Component text, BooleanSupplier condition, Consumer<WorldMapScreen> action, ItemStack stack, String tag) {

        public String getTag() {
            return tag;
        }

        public boolean shouldShow(WorldMapScreen screen) {
            return condition.getAsBoolean();
        }

        public void execute(WorldMapScreen screen) {
            action.accept(screen);
        }
    }
}