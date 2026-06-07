package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.claim.ClaimInfoMenu;
import com.talhanation.recruits.client.gui.worldmap.claim.ClaimRenderer;
import com.talhanation.recruits.client.gui.worldmap.claim.WorldMapClaimController;
import com.talhanation.recruits.client.gui.worldmap.render.MapFramebufferPass;
import com.talhanation.recruits.client.gui.worldmap.render.MapRenderUtil;
import com.talhanation.recruits.client.gui.worldmap.render.WorldMapPlayerRenderer;
import com.talhanation.recruits.client.gui.worldmap.render.WorldMapRenderer;
import com.talhanation.recruits.client.gui.worldmap.route.RouteEditPopup;
import com.talhanation.recruits.client.gui.worldmap.route.RouteNamePopup;
import com.talhanation.recruits.client.gui.worldmap.route.RouteRenderer;
import com.talhanation.recruits.client.gui.worldmap.route.WaypointEditPopup;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.ui.WorldMapContextMenu;
import com.talhanation.recruits.client.gui.worldmap.ui.WorldMapRouteControls;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

public class WorldMapScreen extends Screen {
    private final WorldMapCacheManager mapCache;
    private final WorldMapRenderer mapRenderer;
    private final WorldMapCamera camera;
    private final WorldMapRouteControls routeControls = new WorldMapRouteControls();
    private final Player player;
    private final WorldMapClaimController claimController;
    private static final double DEFAULT_SCALE = 2.0;
    private static final int CHUNK_HIGHLIGHT_COLOR = 0x40FFFFFF;
    private static final int CHUNK_SELECTION_COLOR = 0xFFFFFFFF;
    private static final int DARK_GRAY_BG = 0xFF101010;

    double offsetX = 0, offsetZ = 0;
    public static double scale = DEFAULT_SCALE;
    public double lastMouseX, lastMouseY;
    private boolean isDragging = false;
    private boolean initializedOnce = false;
    private ChunkPos hoveredChunk = null;
    ChunkPos selectedChunk = null;
    private int clickedBlockX = 0, clickedBlockZ = 0;
    private int hoverBlockX = 0, hoverBlockZ = 0;
    private WorldMapContextMenu contextMenu;
    RecruitsClaim selectedClaim = null;
    private ClaimInfoMenu claimInfoMenu;
    public RecruitsRoute selectedRoute;

    // Waypoint drag-to-move state
    @Nullable
    private RecruitsRoute.Waypoint draggingWaypoint = null;
    private BlockPos dragOriginalPos;
    private boolean isDraggingWaypoint = false;
    int snapshotWorldX = 0;
    int snapshotWorldZ = 0;

    private RouteNamePopup routeNamePopup;
    private RouteEditPopup routeEditPopup;
    private WaypointEditPopup waypointEditPopup;

    public WorldMapScreen() {
        super(Component.literal(""));
        this.mapCache = WorldMapCacheManager.getInstance();
        this.mapRenderer = new WorldMapRenderer(mapCache);
        this.camera = new WorldMapCamera(this);
        this.player = Minecraft.getInstance().player;
        this.claimController = new WorldMapClaimController(Minecraft.getInstance(), player, camera);
        this.contextMenu = new WorldMapContextMenu(this);
        this.claimInfoMenu = new ClaimInfoMenu(this);
    }

    public BlockPos getHoveredBlockPos() {
        return new BlockPos(hoverBlockX, resolveSurfaceY(hoverBlockX, hoverBlockZ), hoverBlockZ);
    }

    public BlockPos getClickedBlockPos() {
        return new BlockPos(clickedBlockX, resolveSurfaceY(clickedBlockX, clickedBlockZ), clickedBlockZ);
    }

    private int resolveSurfaceY(int worldX, int worldZ) {
        net.minecraft.client.multiplayer.ClientLevel level = minecraft.level;
        if (level == null) return 64;
        ChunkPos chunk = new ChunkPos(worldX >> 4, worldZ >> 4);
        if (level.getChunkSource().getChunk(chunk.x, chunk.z, false) == null) return 64;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
        return Math.max(y, level.getMinBuildHeight());
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isPlayerAdminAndCreative() {
        return player.hasPermissions(2) && player.isCreative();
    }

    public boolean isPanningMap() {
        return isDragging;
    }

    public double getScale() {
        return scale;
    }

    public void setSelectedChunk(ChunkPos chunk) {
        this.selectedChunk = chunk;
    }

    public ChunkPos selectedChunk() {
        return selectedChunk;
    }

    public RecruitsClaim selectedClaim() {
        return selectedClaim;
    }

    public void clearSelectedChunk() {
        selectedChunk = null;
    }

    public void clearSelectedClaim() {
        selectedClaim = null;
    }

    public int snapshotWorldX() {
        return snapshotWorldX;
    }

    public int snapshotWorldZ() {
        return snapshotWorldZ;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft.level != null && player != null) {
            mapCache.initialize(minecraft.level);
            camera.init(player, initializedOnce);
        }
        initializedOnce = true;
        claimInfoMenu.init();
        ClientManager.loadRoutes();
        initRouteUI();
        routeNamePopup = new RouteNamePopup(this);
        routeEditPopup = new RouteEditPopup(this, player);
        waypointEditPopup = new WaypointEditPopup(this);
    }

    private void initRouteUI() {
        routeControls.refresh(selectedRoute, route -> selectedRoute = route);
    }

    public void refreshRouteUI() {
        initRouteUI();
    }

    // -------------------------------------------------------------------------
    // Route UI
    // -------------------------------------------------------------------------

    private void renderRouteUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        routeControls.render(guiGraphics, font, selectedRoute, mouseX, mouseY, partialTicks);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void centerOnPlayer() {
        camera.centerOnPlayer(player);
    }

    public void resetZoom() {
        camera.resetZoom(player);
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        camera.animate();
        renderBackground(guiGraphics);

        guiGraphics.enableScissor(0, 0, width, height);

        mapRenderer.render(
                guiGraphics,
                width,
                height,
                offsetX,
                offsetZ,
                scale,
                (mapGraphics, frame) -> renderMapOverlays(mapGraphics, mouseX, mouseY, frame));

        guiGraphics.disableScissor();

        renderCoordinatesAndZoom(guiGraphics);
        // Route buttons
        renderRouteUI(guiGraphics, mouseX, mouseY, partialTicks);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        contextMenu.render(guiGraphics, this);

        if (selectedClaim != null && claimInfoMenu.isVisible()) {
            Point p = getClaimInfoMenuPosition(selectedClaim, claimInfoMenu.width, claimInfoMenu.height);
            claimInfoMenu.setPosition(p.x, p.y);
            claimInfoMenu.render(guiGraphics);
        }

        if (routeNamePopup.isVisible()) routeNamePopup.render(guiGraphics, mouseX, mouseY);
        if (routeEditPopup.isVisible()) routeEditPopup.render(guiGraphics, mouseX, mouseY);
        if (waypointEditPopup.isVisible()) waypointEditPopup.render(guiGraphics, mouseX, mouseY);
    }

    private void renderMapOverlays(
            GuiGraphics guiGraphics, int mouseX, int mouseY, MapFramebufferPass.Frame frame) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        double inverseSecondaryScale = 1.0 / frame.secondaryScale();
        pose.translate(frame.secondaryOffsetX(), frame.secondaryOffsetZ(), 0.0);
        pose.scale((float) inverseSecondaryScale, (float) inverseSecondaryScale, 1.0F);
        try {
            if (routeControls.areClaimsTransparent()) {
                ClaimRenderer.renderClaimsOverlayTransparent(
                        guiGraphics, this.selectedClaim, this.offsetX, this.offsetZ, scale);
            } else {
                ClaimRenderer.renderClaimsOverlay(
                        guiGraphics, this.selectedClaim, this.offsetX, this.offsetZ, scale);
            }

            if (contextMenu.isVisible()) {
                String entryTag = contextMenu.getHoveredEntryTag();
                if (entryTag != null) {
                    if (entryTag.contains("bufferzone"))
                        ClaimRenderer.renderBufferZone(guiGraphics, offsetX, offsetZ, scale);
                    if (entryTag.contains("area"))
                        ClaimRenderer.renderAreaPreview(
                                guiGraphics, getClaimArea(selectedChunk), offsetX, offsetZ, scale);
                    if (entryTag.contains("chunk"))
                        ClaimRenderer.renderAreaPreview(
                                guiGraphics, getClaimableChunks(selectedChunk, 16), offsetX, offsetZ, scale);
                }
            }

            WorldMapPlayerRenderer.render(guiGraphics, font, player, offsetX, offsetZ, scale);

            if (selectedChunk != null && (selectedClaim == null || contextMenu.isVisible())) {
                renderChunkOutline(guiGraphics, selectedChunk.x, selectedChunk.z, CHUNK_SELECTION_COLOR);
            }

            if (hoveredChunk != null) renderChunkHighlight(guiGraphics, hoveredChunk.x, hoveredChunk.z);

            if (selectedRoute != null) {
                RouteRenderer.renderRoute(
                        guiGraphics, selectedRoute, offsetX, offsetZ, scale, draggingWaypoint, -1);
                if (isDraggingWaypoint && draggingWaypoint != null) {
                    RouteRenderer.renderDragGhost(guiGraphics, draggingWaypoint, (int) mouseX, (int) mouseY);
                }
            }
        } finally {
            pose.popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, DARK_GRAY_BG);
    }

    // -------------------------------------------------------------------------
    // Waypoint creation
    // -------------------------------------------------------------------------

    public void addWaypointAtClicked() {
        if (selectedRoute == null) return;

        BlockPos pos = getClickedBlockPos();
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);

        // The chunk must also be currently loaded in the level so we can read
        // the real surface Y. If not loaded, resolveSurfaceY would return the
        // fallback value (64) which could place the waypoint underground.
        if (minecraft.level == null
                || minecraft.level.getChunkSource().getChunk(chunk.x, chunk.z, false) == null) return;

        String name = "WP " + (selectedRoute.getWaypoints().size() + 1);
        selectedRoute.addWaypoint(new RecruitsRoute.Waypoint(name, pos, null));
        ClientManager.saveRoute(selectedRoute);
    }

    public boolean canPlaceWaypointAt(int worldX, int worldZ) {
        if (minecraft.level == null) return false;
        ChunkPos chunk = new ChunkPos(worldX >> 4, worldZ >> 4);
        if (!mapCache.isChunkExplored(chunk)) return false;
        return minecraft.level.getChunkSource().getChunk(chunk.x, chunk.z, false) != null;
    }

    public void openWaypointEditPopup(double mouseX, double mouseY) {
        RecruitsRoute.Waypoint wp = RouteRenderer.getWaypointAt(
                selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale);
        if (wp == null) return;
        waypointEditPopup.open(wp);
        contextMenu.close();
    }

    public void removeWaypointAt(double mouseX, double mouseY) {
        if (selectedRoute == null) return;
        RecruitsRoute.Waypoint wp = RouteRenderer.getWaypointAt(
                selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale);
        if (wp != null) {
            selectedRoute.removeWaypoint(wp);
            ClientManager.saveRoute(selectedRoute);
        }
    }

    public boolean isWaypointHoveredAt(double mouseX, double mouseY) {
        if (selectedRoute == null) return false;
        return RouteRenderer.getWaypointAt(selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale) != null;
    }

    private void renderChunkHighlight(GuiGraphics guiGraphics, int chunkX, int chunkZ) {
        double pixelX = offsetX + chunkX * 16.0 * scale;
        double pixelZ = offsetZ + chunkZ * 16.0 * scale;
        double size = 16.0 * scale;
        MapRenderUtil.fill(guiGraphics, pixelX, pixelZ, pixelX + size, pixelZ + size, CHUNK_HIGHLIGHT_COLOR);
    }

    private void renderChunkOutline(GuiGraphics guiGraphics, int chunkX, int chunkZ, int color) {
        double pixelX = offsetX + chunkX * 16.0 * scale;
        double pixelZ = offsetZ + chunkZ * 16.0 * scale;
        double size = 16.0 * scale;
        double thickness = Math.max(1.0, Math.min(2.0, scale));
        MapRenderUtil.fill(guiGraphics, pixelX, pixelZ, pixelX + size, pixelZ + thickness, color);
        MapRenderUtil.fill(guiGraphics, pixelX, pixelZ + size - thickness, pixelX + size, pixelZ + size, color);
        MapRenderUtil.fill(guiGraphics, pixelX, pixelZ, pixelX + thickness, pixelZ + size, color);
        MapRenderUtil.fill(guiGraphics, pixelX + size - thickness, pixelZ, pixelX + size, pixelZ + size, color);
    }

    private void renderCoordinatesAndZoom(GuiGraphics guiGraphics) {
        int hoverY = resolveSurfaceY(hoverBlockX, hoverBlockZ);
        String coords = String.format("X: %d, Y: %d, Z: %d", hoverBlockX, hoverY, hoverBlockZ);
        String zoom = String.format("Zoom: %.1fx", scale);
        String combined = coords + " | " + zoom;
        int textWidth = font.width(combined);
        int bgX = width / 2 - textWidth / 2 - 8;
        int bgY = height - 30;
        int bgWidth = textWidth + 16;
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + 20, 0x80000000);
        guiGraphics.renderOutline(bgX, bgY, bgWidth, 20, 0x40FFFFFF);
        guiGraphics.drawCenteredString(font, combined, width / 2, height - 25, 0xFFFFFF);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Popups get exclusive input
        if (routeNamePopup.isVisible()) return routeNamePopup.mouseClicked(mouseX, mouseY);
        if (routeEditPopup.isVisible()) return routeEditPopup.mouseClicked(mouseX, mouseY);
        if (waypointEditPopup.isVisible()) return waypointEditPopup.mouseClicked(mouseX, mouseY);

        // Route UI buttons
        if (routeControls.isAddButtonHovered(mouseX, mouseY)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeNamePopup.open();
            contextMenu.close();
            return true;
        }

        if (routeControls.isOutlineButtonHovered(mouseX, mouseY)) {
            routeControls.toggleClaimsTransparency();
            return true;
        }

        if (selectedRoute != null && routeControls.isEditButtonHovered(mouseX, mouseY)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeEditPopup.open(selectedRoute);
            return true;
        }

        // Route dropdown
        if (routeControls.isDropdownHovered(mouseX, mouseY)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeControls.clickDropdown(mouseX, mouseY);

            return true;
        }

        if (claimInfoMenu.isVisible() && claimInfoMenu.mouseClicked(mouseX, mouseY, button))
            return true;

        if (contextMenu.isVisible()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button, this)) return true;
        }

        if (hoveredChunk != null) selectedChunk = hoveredChunk;

        RecruitsClaim clickedClaim = ClaimRenderer.getClaimAtPosition(mouseX, mouseY, offsetX, offsetZ, scale);
        if (clickedClaim != null) {
            boolean canInspect =
                    !ClientManager.configFogOfWarEnabled
                            || isPlayerAdminAndCreative()
                            || ClaimRenderer.isClaimExplored(clickedClaim);
            if (canInspect) {
                selectedClaim = clickedClaim;
                claimInfoMenu.openForClaim(selectedClaim, (int) mouseX, (int) mouseY);
            } else {
                selectedClaim = null;
                claimInfoMenu.close();
            }
        } else {
            selectedClaim = null;
            claimInfoMenu.close();
        }

        if (button == 1) {
            double worldX = (mouseX - offsetX) / scale;
            double worldZ = (mouseY - offsetZ) / scale;
            clickedBlockX = (int) Math.floor(worldX);
            clickedBlockZ = (int) Math.floor(worldZ);
            this.contextMenu = new WorldMapContextMenu(this);
            contextMenu.openAt((int) mouseX, (int) mouseY);
            snapshotWorldX = clickedBlockX;
            snapshotWorldZ = clickedBlockZ;
            claimInfoMenu.close();
        }

        if (button == 0) {
            // Start waypoint drag if clicking on a waypoint
            if (!routeNamePopup.isVisible()
                    && !routeEditPopup.isVisible()
                    && !waypointEditPopup.isVisible()
                    && selectedRoute != null) {
                RecruitsRoute.Waypoint wpHit = RouteRenderer.getWaypointAt(
                        selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale);
                if (wpHit != null) {
                    draggingWaypoint = wpHit;
                    dragOriginalPos = wpHit.getPosition();
                    isDraggingWaypoint = true;
                    hoveredChunk = null;
                    selectedChunk = null;
                    return true;
                }
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            isDragging = true;
            camera.beginPanDrag(mouseX, mouseY);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (contextMenu.isVisible()) return false;
        if (button == 0) {
            if (isDraggingWaypoint && draggingWaypoint != null) {
                BlockPos finalPos = draggingWaypoint.getPosition();
                if (canPlaceWaypointAt(finalPos.getX(), finalPos.getZ())) {
                    ClientManager.saveRoute(selectedRoute);
                } else if (dragOriginalPos != null) {
                    draggingWaypoint.setPosition(dragOriginalPos);
                }
                draggingWaypoint = null;
                dragOriginalPos = null;
                isDraggingWaypoint = false;
                return true;
            }
            isDragging = false;
            camera.finishPanDrag(mouseX, mouseY);
        }
        if (claimInfoMenu.isVisible()) claimInfoMenu.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (routeNamePopup.isVisible() || routeEditPopup.isVisible() || waypointEditPopup.isVisible())
            return true;
        if (isDraggingWaypoint && draggingWaypoint != null) {
            hoveredChunk = null;
            selectedChunk = null;
            // Update waypoint world position to follow mouse
            int newWorldX = (int) Math.floor((mouseX - offsetX) / scale);
            int newWorldZ = (int) Math.floor((mouseY - offsetZ) / scale);
            BlockPos newPosition = new BlockPos(newWorldX, resolveSurfaceY(newWorldX, newWorldZ), newWorldZ);
            draggingWaypoint.setPosition(newPosition);
            return true;
        }
        if (isDragging) {
            camera.dragByScreenDelta(mouseX, mouseY, mouseX - lastMouseX, mouseY - lastMouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            if (claimInfoMenu.isVisible()) claimInfoMenu.close();
            return true;
        }
        if (claimInfoMenu.isVisible()) claimInfoMenu.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (routeNamePopup.isVisible() || routeEditPopup.isVisible() || waypointEditPopup.isVisible())
            return true;
        if (claimInfoMenu.isVisible()) claimInfoMenu.close();
        if (contextMenu.isVisible()) contextMenu.close();

        camera.zoomAt(mouseX, mouseY, delta);
        return true;
    }

    public double mouseX, mouseY;

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        routeControls.mouseMoved(mouseX, mouseY);

        // Suppress chunk hover when any popup is open or the dropdown is being hovered
        boolean uiHovered = routeNamePopup.isVisible()
                || routeEditPopup.isVisible()
                || waypointEditPopup.isVisible()
                || routeControls.isDropdownHovered(mouseX, mouseY);

        if (uiHovered) {
            hoveredChunk = null;
            return;
        }

        hoverBlockX = (int) Math.floor((mouseX - offsetX) / scale);
        hoverBlockZ = (int) Math.floor((mouseY - offsetZ) / scale);
        hoveredChunk = new ChunkPos(hoverBlockX >> 4, hoverBlockZ >> 4);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waypointEditPopup.isVisible()) return waypointEditPopup.keyPressed(keyCode);
        if (routeEditPopup.isVisible()) return routeEditPopup.keyPressed(keyCode);
        if (routeNamePopup.isVisible()) return routeNamePopup.keyPressed(keyCode);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (claimInfoMenu.isVisible()) {
                claimInfoMenu.close();
                return true;
            }
            if (contextMenu.isVisible()) {
                contextMenu.close();
                return true;
            }
            onClose();
            return true;
        }

        if (!contextMenu.isVisible() && !claimInfoMenu.isVisible()) {
            double moveSpeed = 40.0 / scale;
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> camera.panByScreenDelta(0.0, moveSpeed);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> camera.panByScreenDelta(0.0, -moveSpeed);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> camera.panByScreenDelta(moveSpeed, 0.0);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> camera.panByScreenDelta(-moveSpeed, 0.0);
                case GLFW.GLFW_KEY_EQUAL -> mouseScrolled(width / 2.0, height / 2.0, 1);
                case GLFW.GLFW_KEY_MINUS -> mouseScrolled(width / 2.0, height / 2.0, -1);
                case GLFW.GLFW_KEY_C -> centerOnPlayer();
                case GLFW.GLFW_KEY_R -> resetZoom();
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (waypointEditPopup.isVisible()) return waypointEditPopup.charTyped(chr);
        if (routeEditPopup.isVisible()) return routeEditPopup.charTyped(chr, modifiers);
        if (routeNamePopup.isVisible()) return routeNamePopup.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (routeNamePopup != null) routeNamePopup.tick();
        if (routeEditPopup != null) routeEditPopup.tick();
        if (waypointEditPopup != null) waypointEditPopup.tick();
    }

    @Override
    public void onClose() {
        camera.rememberCurrentView();
        mapRenderer.close();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // -------------------------------------------------------------------------
    // Route helpers
    // -------------------------------------------------------------------------

    public boolean canAddRoute() {
        return this.selectedRoute != null;
    }

    public void addRoute() {
        routeNamePopup.open();
        contextMenu.close();
    }

    // -------------------------------------------------------------------------
    // Faction / claim helpers
    // -------------------------------------------------------------------------

    public boolean isPlayerFactionLeader() {
        return claimController.isPlayerFactionLeader();
    }

    public boolean isPlayerFactionLeader(RecruitsFaction faction) {
        return claimController.isPlayerFactionLeader(faction);
    }

    public boolean isPlayerClaimLeader() {
        return this.isPlayerClaimLeader(selectedClaim);
    }

    public boolean isPlayerClaimLeader(RecruitsClaim claim) {
        return claimController.isPlayerClaimLeader(claim);
    }

    public List<ChunkPos> getClaimArea(ChunkPos pos) {
        return claimController.getClaimArea(pos);
    }

    public void claimArea() {
        claimController.claimArea(selectedChunk);
    }

    public void claimChunk() {
        claimController.claimChunk(selectedChunk);
    }

    @Nullable
    public RecruitsClaim getNeighborClaim(ChunkPos chunk) {
        return claimController.getNeighborClaim(chunk);
    }

    public void recalculateCenter(RecruitsClaim claim) {
        claimController.recalculateCenter(claim);
    }

    public void centerOnClaim(RecruitsClaim claim) {
        claimController.centerOnClaim(claim);
    }

    public Rectangle getClaimScreenBounds(RecruitsClaim claim) {
        return claimController.getClaimScreenBounds(claim, offsetX, offsetZ, scale);
    }

    public Point getClaimInfoMenuPosition(RecruitsClaim claim, int menuWidth, int menuHeight) {
        return claimController.getClaimInfoMenuPosition(
                claim, menuWidth, menuHeight, width, height, offsetX, offsetZ, scale);
    }

    public boolean canRemoveChunk(ChunkPos pos, RecruitsClaim claim) {
        return claimController.canRemoveChunk(pos, claim);
    }

    public int getClaimCost(RecruitsFaction ownerTeam) {
        return claimController.getClaimCost(ownerTeam);
    }

    public boolean canPlayerPay(int cost, Player player) {
        return claimController.canPlayerPay(cost, player);
    }

    public static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction) {
        return WorldMapClaimController.isInBufferZone(chunk, ownFaction);
    }

    public boolean canClaimChunk(ChunkPos pos) {
        return claimController.canClaimChunk(pos);
    }

    public boolean canClaimArea(List<ChunkPos> areaChunks) {
        return claimController.canClaimArea(selectedChunk, areaChunks);
    }

    public List<ChunkPos> getClaimableChunks(ChunkPos center, int radius) {
        return claimController.getClaimableChunks(center, radius);
    }

    public boolean canClaimChunkRaw(ChunkPos pos) {
        return claimController.canClaimChunkRaw(pos);
    }
}
