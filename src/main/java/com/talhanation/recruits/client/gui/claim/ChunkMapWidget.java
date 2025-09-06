package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.component.BannerRenderer;
import com.talhanation.recruits.client.gui.faction.TeamEditScreen;
import com.talhanation.recruits.client.gui.widgets.ContextMenuEntry;
import com.talhanation.recruits.network.MessageDoPayment;
import com.talhanation.recruits.network.MessageUpdateClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;

import static com.talhanation.recruits.client.ClientManager.ownFaction;



public class ChunkMapWidget extends AbstractWidget {

    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
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
    private double offsetX, offsetZ;
    private boolean dragging = false;
    private final BannerRenderer bannerRenderer;
    private RecruitsClaim selectedClaim;
    private final List<ContextMenuEntry> contextMenuEntries = new ArrayList<>();
    private int contextMenuX, contextMenuY;
    private boolean contextMenuVisible = false;

    public Player player;
    private ChunkPos lastPlayerChunk;
    private static double zoomLevel  = 1.0D;
    public ChunkMapWidget(ClaimMapScreen screen, Player player, int x, int y, int viewRadius) {
        super(x, y, (viewRadius*2+1)*16, (viewRadius*2+1)*16, Component.empty());
        this.screen = screen;
        this.viewRadius = viewRadius;
        this.cellSize = 16;//DO NOT CHANGE
        this.bannerRenderer = new BannerRenderer(null);
        this.bannerRenderer.setRecruitsTeam(ownFaction);
        this.player = player;

        this.center = player.chunkPosition();
        centerOnPlayer();
    }

    public void tick() {
        if(player.tickCount % 20 != 0) return;
        // center update wie gehabt
        ChunkPos currentChunk = player.chunkPosition();
        if (lastPlayerChunk == null || !lastPlayerChunk.equals(currentChunk)) {
            lastPlayerChunk = currentChunk;
            this.center = currentChunk;
            centerOnPlayer();
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || center == null) return;

        ResourceLocation dim = level.dimension().location();

        // 1) PRIORITY: immer neu generieren (und speichern/überschreiben) innerhalb radius 10, sortiert nach Distanz (nächste zuerst)
        final int priorityRadius = 10;
        List<ChunkPos> priorityList = new ArrayList<>();
        for (int dx = -priorityRadius; dx <= priorityRadius; dx++) {
            for (int dz = -priorityRadius; dz <= priorityRadius; dz++) {
                priorityList.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        // sortiere nach quadratischer Distanz (nächste zuerst)
        priorityList.sort(Comparator.comparingInt(p -> {
            int ddx = p.x - center.x;
            int ddz = p.z - center.z;
            return ddx * ddx + ddz * ddz;
        }));

        final double minMeaningfulRatio = 0.05; // anpassen

        for (ChunkPos pos : priorityList) {
            try {
                // Falls bereits im Cache vorhanden: close, wir werden neu prüfen/generieren
                ChunkPreview old = chunkImageCache.get(pos);
                if (old != null) {
                    try { old.close(); } catch (Exception ignored) {}
                }
                // Versuche zu generieren (synchron)
                ChunkPreview generated = new ChunkPreview(level, pos);
                NativeImage generatedImg = generated.getNativeImage();

                boolean meaningful = generated.imageIsMeaningful(minMeaningfulRatio);
                boolean hasSaved = ChunkMapPersistence.chunkExists(dim, pos);

                if (meaningful) {
                    // sinnvoll -> speichern und in Cache aufnehmen
                    try { ChunkMapPersistence.saveChunk(dim, pos, generatedImg); }
                    catch (Exception e) { e.printStackTrace(); }
                    chunkImageCache.put(pos, generated); // keep generated
                } else {
                    // nicht sinnvoll
                    if (hasSaved) {
                        // lade statt überschreiben und verwende gespeichertes Bild
                        Optional<NativeImage> maybe = ChunkMapPersistence.loadChunk(dim, pos);
                        if (maybe.isPresent()) {
                            ChunkPreview fromDisk = ChunkPreview.fromNativeImage(level, pos, maybe.get());
                            // free generated texture
                            try { generated.close(); } catch (Exception ignored) {}
                            chunkImageCache.put(pos, fromDisk);
                        } else {
                            // gespeicherte Datei angeblich vorhanden, aber nicht ladbar -> verwende generated, aber NICHT speichern
                            chunkImageCache.put(pos, generated);
                        }
                    } else {
                        // kein gespeichertes Bild vorhanden -> benutze generated, aber speichere NICHT (um keine schwarzen NBT zu erzeugen)
                        chunkImageCache.put(pos, generated);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // im Fehlerfall nichts überschreiben: wenn es eine gespeicherte Datei gab, lade sie
                try {
                    if (ChunkMapPersistence.chunkExists(dim, pos)) {
                        Optional<NativeImage> maybe = ChunkMapPersistence.loadChunk(dim, pos);
                        if (maybe.isPresent()) {
                            ChunkPreview fromDisk = ChunkPreview.fromNativeImage(level, pos, maybe.get());
                            chunkImageCache.put(pos, fromDisk);
                        }
                    }
                } catch (Exception e2) { e2.printStackTrace(); }
            }
        }

        // 2) Sichtbarer Bereich: sicherstellen, dass alle sichtbaren Chunks im Cache sind.
        //    (Falls bereits generiert/überschrieben durch Priorität, wird containsKey true.)
        int renderMargin = 1;
        for (int dx = -viewRadius - renderMargin; dx <= viewRadius + renderMargin; dx++) {
            for (int dz = -viewRadius - renderMargin; dz <= viewRadius + renderMargin; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);

                if (chunkImageCache.containsKey(pos)) continue; // bereits vorhanden (evtl. durch Priorität)

                // 1) versuchen zu laden von disk
                try {
                    Optional<NativeImage> maybe = ChunkMapPersistence.loadChunk(dim, pos);
                    if (maybe.isPresent()) {
                        ChunkPreview cm = ChunkPreview.fromNativeImage(level, pos, maybe.get());
                        chunkImageCache.put(pos, cm);
                        continue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 2) nicht gefunden -> synchron generieren & speichern (aber nur für sichtbare Region)
                try {
                    ChunkPreview cm = new ChunkPreview(level, pos);
                    chunkImageCache.put(pos, cm);

                    NativeImage img = cm.getNativeImage();
                    if (img != null) {
                        ChunkMapPersistence.saveChunk(dim, pos, img);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void centerOnPlayer() {
        if (player == null || center == null) return;

        double scaledCellSize = cellSize * zoomLevel; // pixels per chunk (aktuell)
        ChunkPos playerChunk = player.chunkPosition();

        // Player local block coords inside the chunk (0..15 + fraction)
        double localInChunkXBlocks = player.getX() - playerChunk.getMinBlockX();
        double localInChunkZBlocks = player.getZ() - playerChunk.getMinBlockZ();

        // convert block-units to pixel-units (1 block => scaledCellSize / 16 pixels)
        double pixelsPerBlock = scaledCellSize / 16.0;
        double localInChunkX = localInChunkXBlocks * pixelsPerBlock;
        double localInChunkZ = localInChunkZBlocks * pixelsPerBlock;

        double widgetCenterX = getWidth() / 2.0;
        double widgetCenterY = getHeight() / 2.0;

        // offset so that player's pixel position is at widget center
        offsetX = widgetCenterX - (double) viewRadius * scaledCellSize - localInChunkX;
        offsetZ = widgetCenterY - (double) viewRadius * scaledCellSize - localInChunkZ;

        this.center = playerChunk;
    }
    public static final Map<ChunkPos, ChunkPreview> chunkImageCache = new HashMap<>();

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || center == null) return;

        double scaledCellSize = cellSize * zoomLevel;

        double baseX = getX() + offsetX + (double) viewRadius * scaledCellSize;
        double baseY = getY() + offsetZ + (double) viewRadius * scaledCellSize;

        renderBackground(guiGraphics);

        renderChunksAndClaims(guiGraphics, level, baseX, baseY, mouseX, mouseY, scaledCellSize);

        renderSelectedChunkBorder(guiGraphics, scaledCellSize);

        renderContextMenu(guiGraphics, mc, mouseX, mouseY);

        renderSelectedClaimPanel(guiGraphics, mc);

        renderPlayerIconAt(guiGraphics, baseX, baseY, scaledCellSize);
    }

    private void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFF555555);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF222222);
    }

    private void renderChunksAndClaims(GuiGraphics guiGraphics, ClientLevel level, double baseX, double baseY, int mouseX, int mouseY, double cellSize) {
        int renderMargin = 1;
        hoverChunk = null;

        int intCellSize = Math.max(1, (int) Math.round(cellSize)); // Sicherstellen, dass Zellgröße sinnvoll ist

        for (int dx = -viewRadius - renderMargin; dx <= viewRadius + renderMargin; dx++) {
            for (int dz = -viewRadius - renderMargin; dz <= viewRadius + renderMargin; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                int px = (int) Math.round(baseX + dx * cellSize);
                int py = (int) Math.round(baseY + dz * cellSize);

                boolean hovered = mouseX >= px && mouseX < px + intCellSize && mouseY >= py && mouseY < py + intCellSize;
                if (hovered) hoverChunk = pos;

                // KEINE Erzeugung hier! Nur rendern, wenn im Cache vorhanden
                ChunkPreview preview = chunkImageCache.get(pos);
                if (preview != null) {
                    // nearest neighbor sicherstellen (keine Filterung)
                    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

                    preview.draw(guiGraphics, px, py, intCellSize, intCellSize, hovered);
                } else {
                    // Platzhalter zeichnen (z.B. komplett schwarz)
                    guiGraphics.fill(px, py, px + intCellSize, py + intCellSize, 0xFF000000);
                }

                renderClaimOverlay(guiGraphics, pos, px, py, cellSize);
            }
        }
    }


    private void renderPlayerIconAt(GuiGraphics guiGraphics, double baseX, double baseY, double cellSize) {
        // pChunk: chunk wo der player gerade ist
        ChunkPos pChunk = player.chunkPosition();

        // local in block units (0..15 + fraction)
        double localBlocksX = player.getX() - pChunk.getMinBlockX();
        double localBlocksZ = player.getZ() - pChunk.getMinBlockZ();

        // convert to pixel-units using cellSize = pixels per chunk
        double pixelsPerBlock = cellSize / 16.0;
        double localPixelX = localBlocksX * pixelsPerBlock;
        double localPixelZ = localBlocksZ * pixelsPerBlock;

        int chunkDx = pChunk.x - center.x;
        int chunkDz = pChunk.z - center.z;

        // final screen position
        int playerScreenX = (int) Math.round(baseX + chunkDx * cellSize + localPixelX);
        int playerScreenY = (int) Math.round(baseY + chunkDz * cellSize + localPixelZ);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(playerScreenX, playerScreenY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));
        pose.scale(5.0f, 5.0f, 1.0f);

        // Vanilla Map Player-Icon UV (wie vorher)
        int iconIndex = 0;
        float u0 = (iconIndex % 16) / 16f;
        float v0 = (iconIndex / 16) / 16f;
        float u1 = u0 + 1f / 16f;
        float v1 = v0 + 1f / 16f;

        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.text(MAP_ICONS));
        Matrix4f matrix = pose.last().pose();
        int light = 0xF000F0;
        int color = 0xFFFFFFFF;

        consumer.vertex(matrix, -1f, 1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, 1f, 1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, 1f, -1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, -1f, -1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, -1).endVertex();

        pose.popPose();
    }

    private void renderClaimOverlay(GuiGraphics guiGraphics, ChunkPos pos, int px, int py, double cellSize) {
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.containsChunk(pos)) {
                int alpha = 190;
                int rgb = TeamEditScreen.unitColors.get(claim.getOwnerFaction().getUnitColor()).getRGB();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int argb = (alpha << 24) | (r << 16) | (g << 8) | b;

                // ➤ kleine Überlappung gegen Lücken
                double overlap = 0.5; // kannst du auf 1.0 erhöhen, falls immer noch Lücken sichtbar sind

                double x1 = px;
                double y1 = py;
                double x2 = px + cellSize + overlap;
                double y2 = py + cellSize + overlap;

                guiGraphics.fill(
                        (int) Math.floor(x1),
                        (int) Math.floor(y1),
                        (int) Math.ceil(x2),
                        (int) Math.ceil(y2),
                        argb
                );

                // Borders vom ausgewählten Claim
                if (selectedClaim != null && selectedClaim.containsChunk(pos)) {
                    renderSelectedClaimBorder(guiGraphics, pos, px, py, cellSize);
                }

                // Claim-Namen nur am Center zeichnen
                if (claim.getCenter().equals(pos)) {
                    renderClaimName(guiGraphics, claim, px, py, cellSize);
                }

                break;
            }
        }
    }


    private void renderClaimName(GuiGraphics guiGraphics, RecruitsClaim claim, int px, int py, double cellSize) {
        Font font = Minecraft.getInstance().font;
        String name = claim.getName();

        int textWidth = font.width(name);
        int textX = (int) (px + (cellSize / 2) - (textWidth / 2));
        int textY = (int) (py + (cellSize / 2) - 4);

        guiGraphics.drawString(font, name, textX, textY, 0xFFFFFF, true);
    }

    private void renderSelectedClaimBorder(GuiGraphics guiGraphics, ChunkPos pos, int px, int py, double cellSize) {
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

        if (top) guiGraphics.fill(px, py, (int) (px + cellSize), py + 1, borderColor);
        if (bottom) guiGraphics.fill(px, (int) (py + cellSize - 1), (int) (px + cellSize), (int) (py + cellSize), borderColor);
        if (left) guiGraphics.fill(px, py, px + 1, (int) (py + cellSize), borderColor);
        if (right) guiGraphics.fill((int) (px + cellSize - 1), py, (int) (px + cellSize), (int) (py + cellSize), borderColor);
    }

    private void renderSelectedChunkBorder(GuiGraphics guiGraphics, double cellSize) {
        if (selectedChunk == null) return;

        int dx = selectedChunk.x - center.x + (int)(offsetX / cellSize);
        int dz = selectedChunk.z - center.z + (int)(offsetZ / cellSize);

        int px = (int) (getX() + (dx + viewRadius) * cellSize + (int)(offsetX % cellSize));
        int py = (int) (getY() + (dz + viewRadius) * cellSize + (int)(offsetZ % cellSize));

        int glowColor = 0xFFFFFFFF;

        guiGraphics.fill(px, py, (int) (px + cellSize), py + 1, glowColor); // top
        guiGraphics.fill(px, (int) (py + cellSize - 1), (int) (px + cellSize), (int) (py + cellSize), glowColor); // bottom
        guiGraphics.fill(px, py, px + 1, (int) (py + cellSize), glowColor); // left
        guiGraphics.fill((int) (px + cellSize - 1), py, (int) (px + cellSize), (int) (py + cellSize), glowColor); // right
    }

    private void renderContextMenu(GuiGraphics guiGraphics, Minecraft mc, int mouseX, int mouseY) {
        if (!contextMenuVisible || contextMenuEntries.isEmpty()) return;

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

    private void renderSelectedClaimPanel(GuiGraphics guiGraphics, Minecraft mc) {
        if (selectedClaim == null) return;

        Font font = mc.font;
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

    private void openContextMenu(ChunkPos chunk) {
        contextMenuEntries.clear();
        if(ownFaction == null) return;

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
        if(ownFaction == null) return;
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
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(claim));
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

    public void zoom(double delta, double mouseX, double mouseY) {
        double oldZoom = zoomLevel;
        double newZoom = Mth.clamp(zoomLevel + delta * 0.1, 0.5, 4.0); // min 0.5x, max 4x
        if (newZoom == oldZoom) return;

        double oldScaled = cellSize * oldZoom;
        double newScaled = cellSize * newZoom;

        // Base = Bildschirm-X des (viewRadius, viewRadius) center-chunks (oben links = getX())
        double baseXOld = getX() + offsetX + (double) viewRadius * oldScaled;
        double baseYOld = getY() + offsetZ + (double) viewRadius * oldScaled;

        // Welt-Position (in chunk-pixel-einheiten, float) unter der Maus vor dem Zoom
        double worldPosX = (mouseX - baseXOld) / oldScaled; // kann negativ sein
        double worldPosY = (mouseY - baseYOld) / oldScaled;

        // Berechne neuen offset so dass worldPos unter der Maus gleich bleibt:
        // mouseX = getX() + offsetXNew + (viewRadius + worldPosX) * newScaled
        double offsetXNew = mouseX - getX() - (viewRadius + worldPosX) * newScaled;
        double offsetZNew = mouseY - getY() - (viewRadius + worldPosY) * newScaled;

        // Übernehme neue Werte
        zoomLevel = newZoom;
        offsetX = offsetXNew;
        offsetZ = offsetZNew;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // delta ist positive beim Scroll hoch (typisch); wir wollen intuitiv zoomen
        zoom(delta, mouseX, mouseY);
        return true;
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

    public boolean canClaimChunk(ChunkPos chunk, RecruitsFaction team, List<RecruitsClaim> allClaims, boolean neighborSameFactionRequired) {
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

    public boolean canClaimChunks(List<ChunkPos> chunksToClaim, RecruitsFaction team, List<RecruitsClaim> allClaims) {
        for (ChunkPos pos : chunksToClaim) {
            if (!canClaimChunk(pos, team, allClaims, false)) return false;
        }
        return true;
    }

    public void claimChunk(ChunkPos centerChunk, RecruitsFaction ownTeam, List<RecruitsClaim> allClaims) {
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

    public void claimArea(ChunkPos centerChunk, RecruitsFaction team, List<RecruitsClaim> allClaims) {
        List<ChunkPos> area = new ArrayList<>();

        int range = 2;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                area.add(new ChunkPos(centerChunk.x + dx, centerChunk.z + dz));
            }
        }

        if (!canClaimChunks(area, team, allClaims)) return;

        RecruitsClaim newClaim = new RecruitsClaim(team.getTeamDisplayName(), team);
        for (ChunkPos pos : area) {
            newClaim.addChunk(pos);
        }

        newClaim.setCenter(centerChunk);
        newClaim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownFaction));

        ClientManager.recruitsClaims.add(newClaim);
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), screen.getClaimCost(ownFaction)));
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
