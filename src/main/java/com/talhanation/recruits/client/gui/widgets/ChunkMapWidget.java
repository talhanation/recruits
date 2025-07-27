package com.talhanation.recruits.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.claim.ChunkMiniMap;
import com.talhanation.recruits.client.gui.claim.ClaimMapScreen;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChunkMapWidget extends AbstractWidget {

    private final ClaimMapScreen screen;
    private final int viewRadius;
    private final int cellSize;
    private ChunkPos center;
    @Nullable
    private ChunkPos selectedChunk = null;
    @Nullable
    private ChunkPos hoverChunk;
    private double offsetX = 0, offsetZ = 0;
    private int lastMouseX, lastMouseY;
    private boolean dragging = false;
    private BannerRenderer bannerRenderer;
    private RecruitsTeam selectedFaction;
    private List<ContextMenuEntry> contextMenuEntries = new ArrayList<>();
    private int contextMenuX, contextMenuY;
    private boolean contextMenuVisible = false;
    private List<RecruitsClaim> recruitsClaims;

    public ChunkMapWidget(ClaimMapScreen screen, int x, int y, int viewRadius) {
        super(x, y, (viewRadius*2+1)*16, (viewRadius*2+1)*16, Component.empty());
        this.screen = screen;
        this.viewRadius = viewRadius;
        this.cellSize = 16;
        this.bannerRenderer = new BannerRenderer(null);
        this.selectedFaction = ClaimMapScreen.ownFaction;
        this.bannerRenderer.setRecruitsTeam(selectedFaction);
        updateCenter();
    }

    public void updateCenter() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            this.center = player.chunkPosition();
        }
    }

    private final Map<ChunkPos, ChunkMiniMap> chunkImageCache = new HashMap<>();

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFF555555);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF222222);

        int screenHeight = mc.getWindow().getHeight();
        int scale = (int) mc.getWindow().getGuiScale();

        int scissorX = getX() * scale;
        int scissorY = screenHeight - (getY() + getHeight()) * scale;
        int scissorWidth = getWidth() * scale;
        int scissorHeight = getHeight() * scale;

        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        int chunkOffsetX = (int)(offsetX / cellSize);
        int chunkOffsetZ = (int)(offsetZ / cellSize);
        int pixelOffsetX = (int)(offsetX % cellSize);
        int pixelOffsetZ = (int)(offsetZ % cellSize);

        int renderMargin = 1;

        hoverChunk = null;
        for (int dx = -viewRadius - renderMargin; dx <= viewRadius + renderMargin; dx++) {
            for (int dz = -viewRadius - renderMargin; dz <= viewRadius + renderMargin; dz++) {
                int chunkX = center.x + dx - chunkOffsetX;
                int chunkZ = center.z + dz - chunkOffsetZ;
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                int px = getX() + (dx + viewRadius) * cellSize + pixelOffsetX;
                int py = getY() + (dz + viewRadius) * cellSize + pixelOffsetZ;

                boolean hovered = mouseX >= px && mouseX < px + cellSize && mouseY >= py && mouseY < py + cellSize;
                if(hovered) hoverChunk = pos;
                ChunkMiniMap preview = chunkImageCache.computeIfAbsent(pos, p -> new ChunkMiniMap(level, p));
                preview.draw(guiGraphics, px, py, hovered);

            }
        }
        //hier selected chunk rendern in abhängigkeit mit den drag
        if (selectedChunk != null) {
            int dx = selectedChunk.x - center.x + chunkOffsetX;
            int dz = selectedChunk.z - center.z + chunkOffsetZ;

            int px = getX() + (dx + viewRadius) * cellSize + pixelOffsetX;
            int py = getY() + (dz + viewRadius) * cellSize + pixelOffsetZ;

            int glowColor = 0xFFFFD700;

            guiGraphics.fill(px, py, px + cellSize, py + 1, glowColor); // top
            guiGraphics.fill(px, py + cellSize - 1, px + cellSize, py + cellSize, glowColor); // bottom
            guiGraphics.fill(px, py, px + 1, py + cellSize, glowColor); // left
            guiGraphics.fill(px + cellSize - 1, py, px + cellSize, py + cellSize, glowColor); // right
        }

        RenderSystem.disableScissor();

        int panelX = getX() + getWidth() + 10;
        int panelY = getY();
        int panelWidth = 130;
        int panelHeight = 225;

        guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF222222);

        guiGraphics.drawString(Minecraft.getInstance().font, "Claim Name", panelX + 5, panelY + 5, 0xFFFFFF);

        bannerRenderer.renderBanner(guiGraphics,  panelX + 55, panelY + 80, this.width, this.height, 60);

        guiGraphics.drawString(Minecraft.getInstance().font, "Faction: ", panelX + 5, panelY + 130, 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, selectedFaction.getTeamDisplayName(), panelX + 90, panelY + 130, 0xFFFFFF);

        guiGraphics.drawString(Minecraft.getInstance().font, "Player: ", panelX + 5, panelY + 150, 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, selectedFaction.getTeamLeaderName(), panelX + 90, panelY + 150, 0xFFFFFF);

        guiGraphics.drawString(Minecraft.getInstance().font, "Block Placing:   ", panelX + 5, panelY + 170, 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, "true", panelX + 90, panelY + 170, 0xFFFFFF);

        guiGraphics.drawString(Minecraft.getInstance().font, "Block Breaking:", panelX + 5, panelY + 190, 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, "true", panelX + 90, panelY + 190, 0xFFFFFF);

        guiGraphics.drawString(Minecraft.getInstance().font, "Block Interact:", panelX + 5, panelY + 210, 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, "false", panelX + 90, panelY + 210, 0xFFFFFF);

        if (contextMenuVisible && !contextMenuEntries.isEmpty()) {
            int entryHeight = 14;
            int width = 100;
            int height = contextMenuEntries.size() * entryHeight;

            guiGraphics.fill(contextMenuX, contextMenuY, contextMenuX + width, contextMenuY + height, 0xFF000000);
            for (int i = 0; i < contextMenuEntries.size(); i++) {
                ContextMenuEntry entry = contextMenuEntries.get(i);
                int entryY = contextMenuY + i * entryHeight;
                int color = entry.enabled ? 0xFFFFFFFF : 0xFF777777;

                guiGraphics.drawString(Minecraft.getInstance().font, entry.label, contextMenuX + 5, entryY + 2, color);
            }
        }
    }

    private void openContextMenu(int x, int y, ChunkPos chunk) {
        contextMenuEntries.clear();
        contextMenuX = x;
        contextMenuY = y;

        contextMenuEntries.add(new ContextMenuEntry("Claim Chunk", () -> claimChunk(chunk), canClaimChunk(chunk)));
        contextMenuEntries.add(new ContextMenuEntry("Claim Area", () -> claimArea(chunk), canClaimArea(chunk)));

        contextMenuVisible = true;
    }

    private void claimChunk(ChunkPos chunk) {
        screen.claimChunk(chunk);
    }
    private void claimArea(ChunkPos chunk) {
        screen.claimArea(chunk);
    }

    public void setWidth(int w) {
        this.width = w;
    }
    public void setHeight(int h) {
        this.height = h;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_169152_) {
        // Keine Narration
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (button == 1 && hoverChunk != null) {
                selectedChunk = hoverChunk;
                openContextMenu((int) mouseX, (int) mouseY, hoverChunk);
                return true;
            }

            if (button == 0) {
                dragging = true;
                lastMouseX = (int) mouseX;
                lastMouseY = (int) mouseY;
                selectedChunk = hoverChunk;
                contextMenuVisible = false;
                return true;
            }
        }
        contextMenuVisible = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (contextMenuVisible) {
                int entryHeight = 14;
                int width = 100;
                int height = contextMenuEntries.size() * entryHeight;

                if (mouseX >= contextMenuX && mouseX < contextMenuX + width &&
                        mouseY >= contextMenuY && mouseY < contextMenuY + height) {

                    int index = (int)((mouseY - contextMenuY) / entryHeight);
                    if (index >= 0 && index < contextMenuEntries.size()) {
                        ContextMenuEntry entry = contextMenuEntries.get(index);
                        if (entry.enabled) {
                            entry.action.run();
                        }
                    }
                    contextMenuVisible = false;
                    return true;
                }
            }

            dragging = false;
            return true;
        }



        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            double dragFactor = 0.75;
            this.offsetX += dx * dragFactor;
            this.offsetZ += dy * dragFactor;
            return true;
        }
        return false;
    }

    private boolean canClaimChunk(ChunkPos pos) {
        return true; // oder z. B. team.hasClaimRights()
    }

    private boolean canClaimArea(ChunkPos pos) {
        return false;
    }
}
