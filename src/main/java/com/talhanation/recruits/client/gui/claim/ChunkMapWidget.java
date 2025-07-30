package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.claim.ChunkMiniMap;
import com.talhanation.recruits.client.gui.claim.ClaimMapScreen;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.client.gui.widgets.ContextMenuEntry;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.*;


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
    private boolean dragging = false;
    private final BannerRenderer bannerRenderer;
    private RecruitsClaim selectedClaim;
    private final RecruitsTeam ownFaction;
    private final List<ContextMenuEntry> contextMenuEntries = new ArrayList<>();
    private int contextMenuX, contextMenuY;
    private boolean contextMenuVisible = false;

    public Player player;
    Set<RecruitsClaim> claimsToDrawNames = new HashSet<>();
    public ChunkMapWidget(ClaimMapScreen screen, Player player, int x, int y, int viewRadius, RecruitsTeam ownFaction) {
        super(x, y, (viewRadius*2+1)*16, (viewRadius*2+1)*16, Component.empty());
        this.screen = screen;
        this.viewRadius = viewRadius;
        this.cellSize = 16;//DO NOT CHANGE
        this.bannerRenderer = new BannerRenderer(null);
        this.ownFaction = ownFaction;
        this.bannerRenderer.setRecruitsTeam(ownFaction);
        this.player = player;
        this.center = player.chunkPosition();
    }

    public void setWidth(int w) {
        this.width = w;
    }
    public void setHeight(int h) {
        this.height = h;
    }

    public static final Map<ChunkPos, ChunkMiniMap> chunkImageCache = new HashMap<>();

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
                if (hovered) hoverChunk = pos;

                ChunkMiniMap  preview = chunkImageCache.computeIfAbsent(pos, p -> new ChunkMiniMap(level, p));
                preview.draw(guiGraphics, px, py, hovered);

                // CLAIM
                for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                    if (claim.containsChunk(pos)) {
                        int alpha = 190;
                        int rgb = TeamEditScreen.unitColors.get(claim.getOwnerFaction().getUnitColor()).getRGB();
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        int argb = (alpha << 24) | (r << 16) | (g << 8) | b;
                        guiGraphics.fill(px, py, px + cellSize, py + cellSize, argb);

                        claimsToDrawNames.add(claim);

                        if (selectedClaim != null && selectedClaim.containsChunk(pos)) {
                            // Kanten prüfen: oben, unten, links, rechts
                            ChunkPos[] directions = {
                                    new ChunkPos(pos.x, pos.z - 1),
                                    new ChunkPos(pos.x, pos.z + 1),
                                    new ChunkPos(pos.x - 1, pos.z),
                                    new ChunkPos(pos.x + 1, pos.z)
                            };
                            boolean top = !selectedClaim.containsChunk(directions[0]);
                            boolean bottom = !selectedClaim.containsChunk(directions[1]);
                            boolean left = !selectedClaim.containsChunk(directions[2]);
                            boolean right = !selectedClaim.containsChunk(directions[3]);

                            int borderColor = 0xFFFFFFFF;

                            if (top) guiGraphics.fill(px, py, px + cellSize, py + 1, borderColor);
                            if (bottom) guiGraphics.fill(px, py + cellSize - 1, px + cellSize, py + cellSize, borderColor);
                            if (left) guiGraphics.fill(px, py, px + 1, py + cellSize, borderColor);
                            if (right) guiGraphics.fill(px + cellSize - 1, py, px + cellSize, py + cellSize, borderColor);
                        }

                        break;
                    }

                }

            }
        }

        if (selectedChunk != null) {
            int dx = selectedChunk.x - center.x + chunkOffsetX;
            int dz = selectedChunk.z - center.z + chunkOffsetZ;

            int px = getX() + (dx + viewRadius) * cellSize + pixelOffsetX;
            int py = getY() + (dz + viewRadius) * cellSize + pixelOffsetZ;

            int glowColor = 0xFFFFFFFF;

            guiGraphics.fill(px, py, px + cellSize, py + 1, glowColor); // top
            guiGraphics.fill(px, py + cellSize - 1, px + cellSize, py + cellSize, glowColor); // bottom
            guiGraphics.fill(px, py, px + 1, py + cellSize, glowColor); // left
            guiGraphics.fill(px + cellSize - 1, py, px + cellSize, py + cellSize, glowColor); // right
        }


        Font font = Minecraft.getInstance().font;
        for (RecruitsClaim claim : claimsToDrawNames) {
            ChunkPos centerPos = claim.getCenter();

            int dx = centerPos.x - center.x + (int)(offsetX / cellSize);
            int dz = centerPos.z - center.z + (int)(offsetZ / cellSize);

            int px = getX() + (dx + viewRadius) * cellSize + (int)(offsetX % cellSize);
            int py = getY() + (dz + viewRadius) * cellSize + (int)(offsetZ % cellSize);

            String name = claim.getName();
            int textWidth = font.width(name);
            int textX = px + (cellSize / 2) - (textWidth / 2);
            int textY = py + (cellSize / 2) - 4;

            guiGraphics.drawString(font, name, textX, textY, 0xFFFFFF, true);
        }

        // ─── Context-Menü ─────────────────────────
        if (contextMenuVisible && !contextMenuEntries.isEmpty()) {
            int entryHeight = 14;
            int width = 100;
            int height = contextMenuEntries.size() * entryHeight;

            guiGraphics.fill(contextMenuX, contextMenuY, contextMenuX + width, contextMenuY + height, 0xFF000000);

            for (int i = 0; i < contextMenuEntries.size(); i++) {
                ContextMenuEntry entry = contextMenuEntries.get(i);
                int entryY = contextMenuY + i * entryHeight;

                boolean hoveringEntry = mouseX >= contextMenuX && mouseX < contextMenuX + width &&
                        mouseY >= entryY && mouseY < entryY + entryHeight;

                int baseColor = entry.enabled ? 0xFFFFFFFF : 0xFF777777;
                int hoverColor = entry.enabled ? 0xFFAAAAAA : 0xFF555555;
                int color = hoveringEntry ? hoverColor : baseColor;

                guiGraphics.drawString(mc.font, entry.label, contextMenuX + 5, entryY + 2, color);
            }
        }

        RenderSystem.disableScissor();

        int panelX = getX() + width + 10;
        int panelY = getY();
        int panelWidth = 150;
        int panelHeight = 225;
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF222222);

        if(selectedClaim != null){
            guiGraphics.drawString(font, selectedClaim.getName(), panelX + 5, panelY + 5, 0xFFFFFF);
            bannerRenderer.renderBanner(guiGraphics, panelX + 65, panelY + 80, this.width, this.height, 60);

            guiGraphics.drawString(font, "Faction: ", panelX + 5, panelY + 130, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.getOwnerFaction().getTeamDisplayName(), panelX + 90, panelY + 130, 0xFFFFFF);

            guiGraphics.drawString(font, "Player: ", panelX + 5, panelY + 150, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.getPlayerInfo() != null ? selectedClaim.getPlayerInfo().getName() : selectedClaim.getOwnerFaction().getTeamLeaderName(), panelX + 90, panelY + 150, 0xFFFFFF);

            guiGraphics.drawString(font, "Block Placing:   ", panelX + 5, panelY + 170, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockPlacementAllowed() ? "true":"false", panelX + 90, panelY + 170, 0xFFFFFF);

            guiGraphics.drawString(font, "Block Breaking:", panelX + 5, panelY + 190, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockBreakingAllowed() ? "true":"false", panelX + 90, panelY + 190, 0xFFFFFF);

            guiGraphics.drawString(font, "Block Interact:", panelX + 5, panelY + 210, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockInteractionAllowed() ? "true":"false", panelX + 90, panelY + 210, 0xFFFFFF);
        }


    }


    private void openContextMenu(ChunkPos chunk) {
        contextMenuEntries.clear();

        int chunkOffsetX = (int)(offsetX / cellSize);
        int chunkOffsetZ = (int)(offsetZ / cellSize);
        int pixelOffsetX = (int)(offsetX % cellSize);
        int pixelOffsetZ = (int)(offsetZ % cellSize);

        int dx = chunk.x - center.x + chunkOffsetX;
        int dz = chunk.z - center.z + chunkOffsetZ;

        int px = getX() + (dx + viewRadius) * cellSize + pixelOffsetX;
        int py = getY() + (dz + viewRadius) * cellSize + pixelOffsetZ;

        contextMenuX = px + cellSize + 5; // rechts neben dem Chunk
        contextMenuY = py;

        contextMenuEntries.add(new ContextMenuEntry("Claim Chunk", () -> claimChunk(chunk, ownFaction, ClientManager.recruitsClaims), canClaimChunk(chunk, ownFaction, ClientManager.recruitsClaims, true)));
        contextMenuEntries.add(new ContextMenuEntry("Claim Area", () -> claimArea(chunk, ownFaction, ClientManager.recruitsClaims), canClaimChunks(getClaimArea(chunk), ownFaction, ClientManager.recruitsClaims)));

        contextMenuVisible = true;
    }

    private void openClaimContextMenu(RecruitsClaim claim, ChunkPos savedHoverChunk) {
        contextMenuEntries.clear();

        ChunkPos rightTop = claim.getClaimedChunks().stream()
                .max(Comparator.<ChunkPos>comparingInt(pos -> pos.x)
                        .thenComparingInt(pos -> pos.z))
                .orElse(claim.getCenter());

        int chunkOffsetX = (int)(offsetX / cellSize);
        int chunkOffsetZ = (int)(offsetZ / cellSize);
        int pixelOffsetX = (int)(offsetX % cellSize);
        int pixelOffsetZ = (int)(offsetZ % cellSize);

        int dx = rightTop.x - center.x + chunkOffsetX;
        int dz = rightTop.z - center.z + chunkOffsetZ;

        int px = getX() + (dx + viewRadius) * cellSize + pixelOffsetX;
        int py = getY() + (dz + viewRadius) * cellSize + pixelOffsetZ;

        contextMenuX = px + cellSize + 5;
        contextMenuY = py;
        //OTHERS CLAIM
        if(!claim.getOwnerFaction().getStringID().equals(ownFaction.getStringID())){
            contextMenuEntries.add(new ContextMenuEntry("Diplomacy",
                    () -> screen.openDiplomacyOf(claim.getOwnerFaction()), true));
        }
        else {
            //OWN CLAIM
            if(player.getUUID().equals(ownFaction.getTeamLeaderUUID()) || player.getUUID().equals(claim.getPlayerInfo().getUUID())){
                contextMenuEntries.add(new ContextMenuEntry("Edit Claim",
                        () -> screen.openClaimEditScreen(claim), true));

                contextMenuEntries.add(new ContextMenuEntry("Remove Chunk",
                        () -> {
                            claim.removeChunk(savedHoverChunk);
                            recalculateCenter(claim);
                        },
                        canRemoveChunk(savedHoverChunk, claim)));
            }
        }

        contextMenuVisible = true;
    }

    private List<ChunkPos> getClaimArea(ChunkPos center) {
        List<ChunkPos> list = new ArrayList<>();

        int range = 2;
        for(int i = -range; i < range; i++){
            for(int k = -range; k < range; k++){
                ChunkPos newPos = new ChunkPos(center.x + i, center.z + k);
                list.add(newPos);
            }
        }
        return list;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_169152_) {
        // Keine Narration
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (button == 1 && hoverChunk != null) {
                //Right-click on claim
                for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                    if (claim.containsChunk(hoverChunk)) {
                        selectedClaim = claim;
                        openClaimContextMenu(selectedClaim, hoverChunk);
                        return true;
                    }
                }
                //Right-Click on chunk
                selectedChunk = hoverChunk;
                selectedClaim = null;
                openContextMenu(hoverChunk);
                return true;
            }

            if (button == 0) {
                dragging = true;
                //Left-click on context menu
                if (contextMenuVisible) {
                    int entryHeight = 14;
                    int width = 100;
                    int height = contextMenuEntries.size() * entryHeight;

                    if (mouseX >= contextMenuX && mouseX < contextMenuX + width &&
                            mouseY >= contextMenuY && mouseY < contextMenuY + height) {

                        int index = (int) ((mouseY - contextMenuY) / entryHeight);
                        if (index >= 0 && index < contextMenuEntries.size()) {
                            ContextMenuEntry entry = contextMenuEntries.get(index);
                            if (entry.enabled) {
                                entry.action.run();
                            }
                        }
                        contextMenuVisible = false;
                        return true;
                    }

                    contextMenuVisible = false;
                    return true;
                }

                //Left-click on claim -> select claim
                for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                    if (claim.containsChunk(hoverChunk)) {
                        selectedClaim = claim;
                        selectedChunk = null;
                        return true;
                    }
                }

                //Left-click on chunk
                selectedChunk = hoverChunk;
                selectedClaim = null;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
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

    public boolean canClaimChunk(ChunkPos chunk, RecruitsTeam team, List<RecruitsClaim> allClaims, boolean neighborSameFactionRequired) {
        for (RecruitsClaim claim : allClaims) {
            for (ChunkPos chunkPos : claim.getClaimedChunks()) {
                if (chunkPos.equals(chunk)) {
                    return false;
                }

                int dx = Math.abs(chunkPos.x - chunk.x);
                int dz = Math.abs(chunkPos.z - chunk.z);

                if (dx <= 5 && dz <= 5 && !claim.getOwnerFaction().getStringID().equals(team.getStringID())) {
                    return false; // zu nah an fremdem Claim
                }
            }
        }

        if (neighborSameFactionRequired) {
            RecruitsClaim neighborClaim = getNeighborClaim(chunk, allClaims);
            return neighborClaim != null && neighborClaim.getOwnerFaction().getStringID().equals(team.getStringID());
        }

        return true;
    }

    @Nullable
    public RecruitsClaim getNeighborClaim(ChunkPos chunk, List<RecruitsClaim> allClaims) {
        ChunkPos[] neighbors = new ChunkPos[] {
                new ChunkPos(chunk.x + 1, chunk.z),
                new ChunkPos(chunk.x - 1, chunk.z),
                new ChunkPos(chunk.x, chunk.z + 1),
                new ChunkPos(chunk.x, chunk.z - 1)
        };

        for (ChunkPos neighbor : neighbors) {
            for (RecruitsClaim claim : allClaims) {
                if (claim.containsChunk(neighbor)) {
                    return claim;
                }
            }
        }

        return null;
    }

    public boolean canClaimChunks(List<ChunkPos> chunksToClaim, RecruitsTeam team, List<RecruitsClaim> allClaims) {
        for (ChunkPos pos : chunksToClaim) {
            if (!canClaimChunk(pos, team, allClaims, false)) return false;
        }
        return true;
    }

    public void claimChunk(ChunkPos centerChunk, RecruitsTeam ownTeam, List<RecruitsClaim> allClaims) {
        RecruitsClaim neighborClaim = getNeighborClaim(centerChunk, allClaims);
        if(neighborClaim == null) return;

        String ownerID = ownTeam.getStringID();
        String neighborID = neighborClaim.getOwnerFaction().getStringID();
        if(!Objects.equals(ownerID, neighborID)) return;

        for(RecruitsClaim claim : allClaims){
            if(claim.equals(neighborClaim)){
                neighborClaim.addChunk(centerChunk);
                this.recalculateCenter(neighborClaim);
                break;
            }
        }
    }

    public void claimArea(ChunkPos centerChunk, RecruitsTeam team, List<RecruitsClaim> allClaims) {
        List<ChunkPos> area = new ArrayList<>();

        int range = 2;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                area.add(new ChunkPos(centerChunk.x + dx, centerChunk.z + dz));
            }
        }

        if (!canClaimChunks(area, team, allClaims)) return;

        RecruitsClaim newClaim = new RecruitsClaim("Claim of " + team.getTeamDisplayName(), team);
        for (ChunkPos pos : area) {
            newClaim.addChunk(pos);
        }

        newClaim.setCenter(centerChunk);
        newClaim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString()));

        ClientManager.recruitsClaims.add(newClaim);
    }

    public void recalculateCenter(RecruitsClaim claim) {
        List<ChunkPos> claimedChunks = claim.getClaimedChunks();
        if (claimedChunks.isEmpty()) return;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (ChunkPos pos : claimedChunks) {
            if (pos.x < minX) minX = pos.x;
            if (pos.x > maxX) maxX = pos.x;
            if (pos.z < minZ) minZ = pos.z;
            if (pos.z > maxZ) maxZ = pos.z;
        }

        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;

        claim.setCenter(new ChunkPos(centerX, centerZ));
    }

    public boolean canRemoveChunk(ChunkPos chunk, RecruitsClaim claim) {
        List<ChunkPos> claimedChunks = claim.getClaimedChunks();
        if (!claimedChunks.contains(chunk)) return false;

        int unclaimedNeighborCount = 0;
        ChunkPos[] neighbors = new ChunkPos[] {
                new ChunkPos(chunk.x + 1, chunk.z),
                new ChunkPos(chunk.x - 1, chunk.z),
                new ChunkPos(chunk.x, chunk.z + 1),
                new ChunkPos(chunk.x, chunk.z - 1)
        };

        for (ChunkPos neighbor : neighbors) {
            if (!claimedChunks.contains(neighbor)) {
                unclaimedNeighborCount++;
            }
        }

        return unclaimedNeighborCount >= 2;
    }
}
