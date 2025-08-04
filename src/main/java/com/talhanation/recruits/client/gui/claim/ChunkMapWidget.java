package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.claim.ChunkMiniMap;
import com.talhanation.recruits.client.gui.claim.ClaimMapScreen;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.client.gui.widgets.ContextMenuEntry;
import com.talhanation.recruits.network.MessageDoPayment;
import com.talhanation.recruits.network.MessageToServerRequestUpdateDiplomacyList;
import com.talhanation.recruits.network.MessageUpdateClaim;
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
    private static final Component EDIT_CLAIM = Component.translatable("gui.recruits.claim.editClaim");
    private static final Component REMOVE_CHUNK = Component.translatable("gui.recruits.claim.removeChunk");
    private static final Component FACTION_TEXT = Component.translatable("gui.recruits.claim.faction");
    private static final Component DIPLOMACY_TEXT = Component.translatable("gui.recruits.claim.diplomacy");
    private static final Component TRUE_TEXT = Component.translatable("gui.recruits.claim.true");
    private static final Component FALSE_TEXT = Component.translatable("gui.recruits.claim.false");
    private static final Component PLAYER_TEXT = Component.translatable("gui.recruits.claim.player");
    private static final Component BLOCK_PLACING_TEXT = Component.translatable("gui.recruits.claim.block_placing");
    private static final Component BLOCK_BREAKING_TEXT = Component.translatable("gui.recruits.claim.block_breaking");
    private static final Component BLOCK_INTERACTION_TEXT = Component.translatable("gui.recruits.claim.block_interaction");
    private final ClaimMapScreen screen;
    private final int viewRadius;
    private final int cellSize;
    private ChunkPos center;
    @Nullable
    private ChunkPos selectedChunk = null;
    @Nullable
    private ChunkPos hoverChunk;
    private double offsetX = 0, offsetZ = -100;
    private boolean dragging = false;
    private final BannerRenderer bannerRenderer;
    private RecruitsClaim selectedClaim;
    private final RecruitsTeam ownFaction;
    private final List<ContextMenuEntry> contextMenuEntries = new ArrayList<>();
    private int contextMenuX, contextMenuY;
    private boolean contextMenuVisible = false;

    public Player player;
    private ChunkPos lastPlayerChunk;
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
        chunkImageCache.clear();
    }

    public void tick() {
        ChunkPos currentChunk = player.chunkPosition();
        if (lastPlayerChunk == null || !lastPlayerChunk.equals(currentChunk)) {
            lastPlayerChunk = currentChunk;
            this.center = currentChunk;

            claimsToDrawNames.clear();
        }
    }

    public static final Map<ChunkPos, ChunkMiniMap> chunkImageCache = new HashMap<>();

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (center == null) return;

        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFF555555);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF222222);

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

        if(selectedClaim != null){
            int panelWidth = 135;
            int panelHeight = 225;
            int panelX = width - panelWidth - 2;
            int panelY = height - panelHeight - 2;
            guiGraphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF555555);
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF222222);

            guiGraphics.drawString(font, selectedClaim.getName(), panelX + 5, panelY + 5, 0xFFFFFF);
            bannerRenderer.renderBanner(guiGraphics, panelX + 60, panelY + 80, this.width, this.height, 60);

            guiGraphics.drawString(font, FACTION_TEXT.getString() + ": ", panelX + 5, panelY + 130, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.getOwnerFaction().getTeamDisplayName(), panelX + 90, panelY + 130, 0xFFFFFF);

            guiGraphics.drawString(font, PLAYER_TEXT.getString() + ": ", panelX + 5, panelY + 150, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.getPlayerInfo() != null ? selectedClaim.getPlayerInfo().getName() : selectedClaim.getOwnerFaction().getTeamLeaderName(), panelX + 90, panelY + 150, 0xFFFFFF);

            guiGraphics.drawString(font, BLOCK_PLACING_TEXT.getString() + ": ", panelX + 5, panelY + 170, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockPlacementAllowed() ? TRUE_TEXT.getString() : FALSE_TEXT.getString(), panelX + 90, panelY + 170, 0xFFFFFF);

            guiGraphics.drawString(font, BLOCK_BREAKING_TEXT.getString() + ": ", panelX + 5, panelY + 190, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockBreakingAllowed() ? TRUE_TEXT.getString(): FALSE_TEXT.getString(), panelX + 90, panelY + 190, 0xFFFFFF);

            guiGraphics.drawString(font, BLOCK_INTERACTION_TEXT.getString() + ": ", panelX + 5, panelY + 210, 0xFFFFFF);
            guiGraphics.drawString(font, selectedClaim.isBlockInteractionAllowed() ? TRUE_TEXT.getString(): FALSE_TEXT.getString(), panelX + 90, panelY + 210, 0xFFFFFF);
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

        contextMenuEntries.add(new ContextMenuEntry(ClaimMapScreen.CLAIM_CHUNK.getString(), () -> claimChunk(chunk, ownFaction, ClientManager.recruitsClaims), screen.canPlayerClaim(ClientManager.configValueChunkCost, player) && canClaimChunk(chunk, ownFaction, ClientManager.recruitsClaims, true)));
        contextMenuEntries.add(new ContextMenuEntry(ClaimMapScreen.CLAIM_AREA.getString(), () -> claimArea(chunk, ownFaction, ClientManager.recruitsClaims), screen.canPlayerClaim(screen.getClaimCost(ownFaction), player) && canClaimChunks(getClaimArea(chunk), ownFaction, ClientManager.recruitsClaims)));

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
            Main.SIMPLE_CHANNEL.sendToServer(new MessageToServerRequestUpdateDiplomacyList());
            contextMenuEntries.add(new ContextMenuEntry(DIPLOMACY_TEXT.getString(),
                    () -> screen.openDiplomacyOf(claim.getOwnerFaction()), true));
        }
        else {
            //OWN CLAIM
            if(player.getUUID().equals(ownFaction.getTeamLeaderUUID()) || player.getUUID().equals(claim.getPlayerInfo().getUUID())){
                contextMenuEntries.add(new ContextMenuEntry(EDIT_CLAIM.getString(),
                        () -> screen.openClaimEditScreen(claim), true));

                contextMenuEntries.add(new ContextMenuEntry(REMOVE_CHUNK.getString(),
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
                        this.bannerRenderer.setRecruitsTeam(selectedClaim.getOwnerFaction());
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
                        this.bannerRenderer.setRecruitsTeam(selectedClaim.getOwnerFaction());
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

        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(neighborClaim));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), ClientManager.configValueChunkCost));
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
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), screen.getClaimCost(this.ownFaction)));
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
