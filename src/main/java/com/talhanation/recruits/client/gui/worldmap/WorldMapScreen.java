package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.DropDownMenu;
import com.talhanation.recruits.compat.smallships.SmallShips;
import com.talhanation.recruits.network.MessageDoPayment;
import com.talhanation.recruits.network.MessageUpdateClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.talhanation.recruits.client.ClientManager.ownFaction;

public class WorldMapScreen extends Screen {
    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
    private final WorldMapTileManager tileManager;
    private final WorldMapRenderer mapRenderer;
    private final WorldMapCamera camera;
    private final Player player;
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

    // Route UI
    private static final int ROUTE_UI_X = 10;
    private static final int ROUTE_UI_Y = 10;
    private static final int ROUTE_DROPDOWN_W = 140;
    private static final int ROUTE_BTN_SIZE = 20;
    private static final int ROUTE_BTN_GAP = 3;

    private DropDownMenu<RecruitsRoute> routeDropDown;

    private RouteNamePopup routeNamePopup;
    private RouteEditPopup routeEditPopup;
    private WaypointEditPopup waypointEditPopup;

    public WorldMapScreen() {
        super(Component.literal(""));
        this.contextMenu = new WorldMapContextMenu(this);
        this.claimInfoMenu = new ClaimInfoMenu(this);
        this.tileManager = WorldMapTileManager.getInstance();
        this.mapRenderer = new WorldMapRenderer(tileManager);
        this.camera = new WorldMapCamera(this);
        this.player = Minecraft.getInstance().player;
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

    boolean isPanningMap() {
        return isDragging;
    }

    public double getScale() {
        return scale;
    }

    public void setSelectedChunk(ChunkPos chunk) {
        this.selectedChunk = chunk;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft.level != null && player != null) {
            tileManager.initialize(minecraft.level);
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
        List<RecruitsRoute> routes = ClientManager.getRoutesList();
        List<RecruitsRoute> options = new ArrayList<>();
        options.add(null);
        options.addAll(routes);

        routeDropDown = new DropDownMenu<>(
                selectedRoute,
                ROUTE_UI_X,
                ROUTE_UI_Y,
                ROUTE_DROPDOWN_W,
                ROUTE_BTN_SIZE,
                options,
                r -> r == null ? "-- Route --" : r.getName(),
                r -> {
                    selectedRoute = r;
                }
        );

        routeDropDown.setBgFill(0x80333333);
        routeDropDown.setBgFillHovered(0x80555555);
        routeDropDown.setBgFillSelected(0x80222222);
    }

    public void refreshRouteUI() {
        initRouteUI();
    }

    // -------------------------------------------------------------------------
    // Route buttons (rendered manually to match popup style)
    // -------------------------------------------------------------------------

    private int getAddBtnX() {
        return ROUTE_UI_X + ROUTE_DROPDOWN_W + ROUTE_BTN_GAP;
    }

    private int getOutlineBtnX() {
        return getAddBtnX() + ROUTE_BTN_SIZE + ROUTE_BTN_GAP;
    }

    private int getEditBtnX() {
        return getOutlineBtnX() + ROUTE_BTN_SIZE + ROUTE_BTN_GAP;
    }

    public boolean claimTransparency = false;

    private void renderRouteUI(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderRouteDropdown(guiGraphics, mouseX, mouseY, partialTicks);

        int addX = getAddBtnX();
        renderRouteButton(guiGraphics, mouseX, mouseY, addX, "+", false);

        int outlineX = getOutlineBtnX();
        renderRouteButton(guiGraphics, mouseX, mouseY, outlineX, "\u25A1", claimTransparency);

        // ⚙ button (only active when route selected)
        if (selectedRoute != null) {
            int editX = getEditBtnX();
            renderRouteButton(guiGraphics, mouseX, mouseY, editX, "\u2699", false);
        }
    }

    private void renderRouteButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, String label, boolean selected) {
        boolean hovered = isRouteButtonHovered(mouseX, mouseY, x);
        int bg = selected ? 0x80555555 : (hovered ? 0x80444444 : 0x80222222);
        int color = selected ? 0xFFFFAA00 : 0xFFFFFF;
        guiGraphics.fill(x, ROUTE_UI_Y, x + ROUTE_BTN_SIZE, ROUTE_UI_Y + ROUTE_BTN_SIZE, bg);
        guiGraphics.renderOutline(x, ROUTE_UI_Y, ROUTE_BTN_SIZE, ROUTE_BTN_SIZE, 0x40FFFFFF);
        guiGraphics.drawCenteredString(font, label, x + ROUTE_BTN_SIZE / 2, ROUTE_UI_Y + 6, color);
    }

    private boolean isRouteButtonHovered(double mouseX, double mouseY, int x) {
        return mouseX >= x && mouseX <= x + ROUTE_BTN_SIZE
                && mouseY >= ROUTE_UI_Y && mouseY <= ROUTE_UI_Y + ROUTE_BTN_SIZE;
    }

    // -------------------------------------------------------------------------

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

        mapRenderer.render(guiGraphics, width, height, offsetX, offsetZ, scale);
        if (claimTransparency) {
            ClaimRenderer.renderClaimsOverlayTransparent(guiGraphics, this.selectedClaim, this.offsetX, this.offsetZ, scale);
        } else {
            ClaimRenderer.renderClaimsOverlay(guiGraphics, this.selectedClaim, this.offsetX, this.offsetZ, scale);
        }

        if (contextMenu.isVisible()) {
            String entryTag = contextMenu.getHoveredEntryTag();
            if (entryTag != null) {
                if (entryTag.contains("bufferzone"))
                    ClaimRenderer.renderBufferZone(guiGraphics, offsetX, offsetZ, scale);
                if (entryTag.contains("area"))
                    ClaimRenderer.renderAreaPreview(guiGraphics, getClaimArea(selectedChunk), offsetX, offsetZ, scale);
                if (entryTag.contains("chunk"))
                    ClaimRenderer.renderAreaPreview(guiGraphics, getClaimableChunks(selectedChunk, 16), offsetX, offsetZ, scale);
            }
        }

        if (player != null) renderPlayerPosition(guiGraphics);

        if (selectedChunk != null && (selectedClaim == null || contextMenu.isVisible())) {
            renderChunkOutline(guiGraphics, selectedChunk.x, selectedChunk.z, CHUNK_SELECTION_COLOR);
        }

        if (hoveredChunk != null) renderChunkHighlight(guiGraphics, hoveredChunk.x, hoveredChunk.z);

        if (selectedRoute != null) {
            RouteRenderer.renderRoute(guiGraphics, selectedRoute, offsetX, offsetZ, scale,
                    draggingWaypoint, -1);
            if (isDraggingWaypoint && draggingWaypoint != null) {
                RouteRenderer.renderDragGhost(guiGraphics, draggingWaypoint, (int) mouseX, (int) mouseY);
            }
        }

        guiGraphics.disableScissor();

        renderCoordinatesAndZoom(guiGraphics);
        // Buttons (+ and ⚙)
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

    private void renderRouteDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (routeDropDown == null) return;

        int ddX = ROUTE_UI_X;
        int ddY = ROUTE_UI_Y;
        int ddW = ROUTE_DROPDOWN_W;
        int ddH = ROUTE_BTN_SIZE;

        guiGraphics.fill(ddX, ddY, ddX + ddW, ddY + ddH, 0x80222222);
        guiGraphics.renderOutline(ddX, ddY, ddW, ddH, 0x40FFFFFF);

        if (routeDropDown.isOpen()) {
            routeDropDown.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            String label = selectedRoute != null ? selectedRoute.getName() : "--- Routes ---";
            guiGraphics.drawCenteredString(font, label, ddX + ddW / 2, ddY + (ddH - 8) / 2, 0xFFFFFF);
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
        if (!tileManager.isChunkExplored(chunk)) return false;
        return minecraft.level.getChunkSource().getChunk(chunk.x, chunk.z, false) != null;
    }

    public void openWaypointEditPopup(double mouseX, double mouseY) {
        RecruitsRoute.Waypoint wp = RouteRenderer.getWaypointAt(selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale);
        if (wp == null) return;
        waypointEditPopup.open(wp);
        contextMenu.close();
    }

    public void removeWaypointAt(double mouseX, double mouseY) {
        if (selectedRoute == null) return;
        RecruitsRoute.Waypoint wp = RouteRenderer.getWaypointAt(selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale);
        if (wp != null) {
            selectedRoute.removeWaypoint(wp);
            ClientManager.saveRoute(selectedRoute);
        }
    }

    public boolean isWaypointHoveredAt(double mouseX, double mouseY) {
        if (selectedRoute == null) return false;
        return RouteRenderer.getWaypointAt(selectedRoute, mouseX, mouseY, offsetX, offsetZ, scale) != null;
    }

    // -------------------------------------------------------------------------
    // Player rendering
    // -------------------------------------------------------------------------

    private static final ItemStack BOAT_STACK = new ItemStack(Items.OAK_BOAT);

    private void renderPlayerPosition(GuiGraphics guiGraphics) {
        double playerWorldX = player.getX();
        double playerWorldZ = player.getZ();
        double pixelX = offsetX + playerWorldX * scale;
        double pixelZ = offsetZ + playerWorldZ * scale;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(pixelX, pixelZ, 0);
        if (player.getVehicle() instanceof Boat) renderPlayerBoat(pose, guiGraphics);
        else renderPlayerIcon(pose, guiGraphics);
        pose.popPose();
        renderPlayerNameTag(guiGraphics, pixelX, pixelZ);
    }

    private void renderPlayerBoat(PoseStack pose, GuiGraphics guiGraphics) {
        float yaw = player.getYRot() % 360f;
        if (yaw < -180f) yaw += 360f;
        if (yaw >= 180f) yaw -= 360f;
        boolean flipX = yaw > 0;
        pose.pushPose();
        if (flipX) pose.scale(-1f, 1f, 1f);
        pose.scale(1.5f, 1.5f, 1.5f);
        Lighting.setupForFlatItems();
        ItemStack boat = BOAT_STACK;
        if (Main.isSmallShipsLoaded && player.getVehicle() != null && SmallShips.isSmallShip(player.getVehicle())) {
            boat = SmallShips.getSmallShipsItem();
        }
        RenderSystem.disableCull();
        guiGraphics.renderItem(boat, -8, -8);
        RenderSystem.enableCull();
        pose.popPose();
    }

    private void renderPlayerIcon(PoseStack pose, GuiGraphics guiGraphics) {
        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));
        pose.scale(5.0f, 5.0f, 5.0f);
        int iconIndex = 0;
        float u0 = (iconIndex % 16) / 16f;
        float v0 = (iconIndex / 16) / 16f;
        float u1 = u0 + 1f / 16f;
        float v1 = v0 + 1f / 16f;
        guiGraphics.flush();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.text(MAP_ICONS));
        Matrix4f matrix = pose.last().pose();
        int light = 0xF000F0;
        int color = 0xFFFFFFFF;
        consumer.vertex(matrix, -1f, 1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, 1f, 1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, 1f, -1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, -1f, -1f, 0f).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
    }

    private void renderPlayerNameTag(GuiGraphics guiGraphics, double pixelX, double pixelZ) {
        if (player != null && scale > 1.5) {
            String playerName = player.getName().getString();
            float textScale = (float) Math.min(1.0, scale / 1.25);
            int textWidth = font.width(playerName);
            int textHeight = font.lineHeight;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(pixelX - (textWidth * textScale) / 2.0, pixelZ - (textHeight * textScale) / 2.0 - 10, 0);
            guiGraphics.pose().scale(textScale, textScale, 1.0f);
            guiGraphics.drawString(font, playerName, 0, 0, 0xFFFFFF, false);
            guiGraphics.pose().popPose();
        }
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
        int addBtnX = getAddBtnX();
        int editBtnX = getEditBtnX();
        if (isRouteButtonHovered(mouseX, mouseY, addBtnX)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeNamePopup.open();
            contextMenu.close();
            return true;
        }

        int outlineBtnX = getOutlineBtnX();
        if (isRouteButtonHovered(mouseX, mouseY, outlineBtnX)) {
            claimTransparency = !claimTransparency;
            return true;
        }

        if (selectedRoute != null && isRouteButtonHovered(mouseX, mouseY, editBtnX)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeEditPopup.open(selectedRoute);
            return true;
        }

        // Route dropdown
        if (routeDropDown != null && routeDropDown.isMouseOver(mouseX, mouseY)) {
            hoveredChunk = null;
            selectedChunk = null;
            routeDropDown.onMouseClick(mouseX, mouseY);

            return true;
        }

        if (claimInfoMenu.isVisible() && claimInfoMenu.mouseClicked(mouseX, mouseY, button)) return true;

        if (contextMenu.isVisible()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button, this)) return true;
        }

        if (hoveredChunk != null) selectedChunk = hoveredChunk;

        RecruitsClaim clickedClaim = ClaimRenderer.getClaimAtPosition(mouseX, mouseY, offsetX, offsetZ, scale);
        if (clickedClaim != null) {
            boolean canInspect = !ClientManager.configFogOfWarEnabled
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
            if (!routeNamePopup.isVisible() && !routeEditPopup.isVisible() && !waypointEditPopup.isVisible() && selectedRoute != null) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (routeNamePopup.isVisible() || routeEditPopup.isVisible() || waypointEditPopup.isVisible()) return true;
        if (isDraggingWaypoint && draggingWaypoint != null) {
            hoveredChunk = null;
            selectedChunk = null;
            // Update waypoint world position to follow mouse
            int newWorldX = (int) Math.floor((mouseX - offsetX) / scale);
            int newWorldZ = (int) Math.floor((mouseY - offsetZ) / scale);
            draggingWaypoint.setPosition(new net.minecraft.core.BlockPos(newWorldX, resolveSurfaceY(newWorldX, newWorldZ), newWorldZ));
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
        if (routeNamePopup.isVisible() || routeEditPopup.isVisible() || waypointEditPopup.isVisible()) return true;
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
        if (routeDropDown != null) routeDropDown.onMouseMove(mouseX, mouseY);

        // Suppress chunk hover when any popup is open or the dropdown is being hovered
        boolean uiHovered = routeNamePopup.isVisible()
                || routeEditPopup.isVisible()
                || waypointEditPopup.isVisible()
                || (routeDropDown != null && routeDropDown.isMouseOver(mouseX, mouseY));

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
        if (routeEditPopup.isVisible())    return routeEditPopup.charTyped(chr, modifiers);
        if (routeNamePopup.isVisible())    return routeNamePopup.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

        @Override
        public void tick() {
            super.tick();
            if (routeNamePopup    != null) routeNamePopup.tick();
            if (routeEditPopup    != null) routeEditPopup.tick();
            if (waypointEditPopup != null) waypointEditPopup.tick();
        }

        public void onClose() {
            camera.rememberCurrentView();
            mapRenderer.close();
            tileManager.flush();
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
            ;
        }

        // -------------------------------------------------------------------------
        // Faction / claim helpers (unchanged)
        // -------------------------------------------------------------------------

        public boolean isPlayerFactionLeader() {
            return this.isPlayerFactionLeader(ownFaction);
        }

        public boolean isPlayerFactionLeader(RecruitsFaction faction){
            if (player == null || faction == null) return false;
            return faction.getTeamLeaderUUID().equals(player.getUUID());
        }

        public boolean isPlayerClaimLeader() {
            return this.isPlayerClaimLeader(selectedClaim);
        }

        public boolean isPlayerClaimLeader(RecruitsClaim claim){
            if (player == null || claim == null) return false;
            return claim.getPlayerInfo().getUUID().equals(player.getUUID());
        }

        public List<ChunkPos> getClaimArea(ChunkPos pos){
            List<ChunkPos> area = new ArrayList<>();
            if (pos == null) return area;
            int range = 2;
            for (int dx = -range; dx <= range; dx++)
                for (int dz = -range; dz <= range; dz++)
                    area.add(new ChunkPos(pos.x + dx, pos.z + dz));
            return area;
        }

        public void claimArea() {
            if (!canPlayerPay(getClaimCost(ownFaction), player)) return;
            if (!ClientManager.configValueIsClaimingAllowed) return;
            if (!isPlayerInOverworld()) return;

            List<ChunkPos> area = getClaimArea(this.selectedChunk);
            RecruitsClaim newClaim = new RecruitsClaim(ownFaction);

            for (ChunkPos pos : area) newClaim.addChunk(pos);
            newClaim.setCenter(selectedChunk);
            newClaim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownFaction));
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), getClaimCost(ownFaction)));
            ClientManager.recruitsClaims.add(newClaim);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
        }

        public void claimChunk() {
            if (!canPlayerPay(ClientManager.configValueChunkCost, player)) return;
            if (!ClientManager.configValueIsClaimingAllowed) return;
            if (!isPlayerInOverworld()) return;
            RecruitsClaim neighborClaim = getNeighborClaim(selectedChunk);
            if (neighborClaim == null) return;
            if (!Objects.equals(ownFaction.getStringID(), neighborClaim.getOwnerFaction().getStringID())) return;
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim.equals(neighborClaim)) {
                    neighborClaim.addChunk(selectedChunk);
                    recalculateCenter(neighborClaim);
                    break;
                }
            }
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), ClientManager.configValueChunkCost));
            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(neighborClaim));
        }

        @Nullable
        public RecruitsClaim getNeighborClaim(ChunkPos chunk){
            ChunkPos[] neighbors = {
                    new ChunkPos(chunk.x + 1, chunk.z), new ChunkPos(chunk.x - 1, chunk.z),
                    new ChunkPos(chunk.x, chunk.z + 1), new ChunkPos(chunk.x, chunk.z - 1)
            };
            for (ChunkPos neighbor : neighbors)
                for (RecruitsClaim claim : ClientManager.recruitsClaims)
                    if (claim.containsChunk(neighbor)) return claim;
            return null;
        }

        public void recalculateCenter(RecruitsClaim claim){
            List<ChunkPos> chunks = claim.getClaimedChunks();
            if (chunks.isEmpty()) return;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (ChunkPos pos : chunks) {
                if (pos.x < minX) minX = pos.x;
                if (pos.x > maxX) maxX = pos.x;
                if (pos.z < minZ) minZ = pos.z;
                if (pos.z > maxZ) maxZ = pos.z;
            }
            claim.setCenter(new ChunkPos((minX + maxX) / 2, (minZ + maxZ) / 2));
        }

        public void centerOnClaim(RecruitsClaim claim){
            if (claim == null || claim.getCenter() == null) return;
            camera.centerOnClaim(claim.getCenter());
        }

        public Rectangle getClaimScreenBounds(RecruitsClaim claim){
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (ChunkPos pos : claim.getClaimedChunks()) {
                minX = Math.min(minX, pos.x);
                maxX = Math.max(maxX, pos.x);
                minZ = Math.min(minZ, pos.z);
                maxZ = Math.max(maxZ, pos.z);
            }
            int x1 = (int) (offsetX + minX * 16 * scale);
            int y1 = (int) (offsetZ + minZ * 16 * scale);
            int x2 = (int) (offsetX + (maxX + 1) * 16 * scale);
            int y2 = (int) (offsetZ + (maxZ + 1) * 16 * scale);
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }

        public Point getClaimInfoMenuPosition(RecruitsClaim claim,int menuWidth, int menuHeight){
            Rectangle bounds = getClaimScreenBounds(claim);
            int margin = 10;
            int x = bounds.x + bounds.width + margin;
            int y = bounds.y + bounds.height / 2 - menuHeight / 2;
            if (x + menuWidth > width) x = bounds.x - menuWidth - margin;
            if (y < 10) y = 10;
            if (y + menuHeight > height - 10) y = height - menuHeight - 10;
            return new Point(x, y);
        }

        public boolean canRemoveChunk(ChunkPos pos, RecruitsClaim claim){
            if (pos == null || ownFaction == null) return false;
            if (isPlayerTooFar(pos)) return false;
            List<ChunkPos> claimedChunks = claim.getClaimedChunks();
            if (!claimedChunks.contains(pos)) return false;
            int unclaimedNeighbors = 0;
            for (ChunkPos n : new ChunkPos[]{
                    new ChunkPos(pos.x + 1, pos.z), new ChunkPos(pos.x - 1, pos.z),
                    new ChunkPos(pos.x, pos.z + 1), new ChunkPos(pos.x, pos.z - 1)}) {
                if (!claimedChunks.contains(n)) unclaimedNeighbors++;
            }
            return unclaimedNeighbors >= 2;
        }

        private boolean isPlayerTooFar(ChunkPos pos){
            if (pos == null) return true;
            int diffX = Math.abs(player.chunkPosition().x) - Math.abs(pos.x);
            int diffZ = Math.abs(player.chunkPosition().z) - Math.abs(pos.z);
            return Math.abs(diffZ) > 4 || Math.abs(diffX) > 4;
        }

        public int getClaimCost(RecruitsFaction ownerTeam){
            if (!ClientManager.configValueCascadeClaimCost) return ClientManager.configValueClaimCost;
            int amount = 1;
            if (ownerTeam != null)
                for (RecruitsClaim claim : ClientManager.recruitsClaims)
                    if (claim.getOwnerFaction().getStringID().equals(ownerTeam.getStringID())) amount++;
            return amount * ClientManager.configValueClaimCost;
        }

        public boolean canPlayerPay( int cost, Player player){
            return player.isCreative() || cost <= player.getInventory().countItem(ClientManager.currencyItemStack.getItem());
        }

        public static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction){
            if (ownFaction == null) return false;
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim.getOwnerFaction() == null || claim.getOwnerFaction().getStringID().equals(ownFaction.getStringID()))
                    continue;
                for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                    int dx = Math.abs(chunk.x - claimChunk.x);
                    int dz = Math.abs(chunk.z - claimChunk.z);
                    if (dx <= 3 && dz <= 3 && !(dx == 0 && dz == 0)) return true;
                }
            }
            return false;
        }

        private boolean isPlayerInOverworld () {
            return minecraft.level != null && minecraft.level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
        }

        public boolean canClaimChunk(ChunkPos pos){
            if (!ClientManager.configValueIsClaimingAllowed || pos == null || ClientManager.ownFaction == null)
                return false;
            if (isPlayerTooFar(pos)) return false;
            for (RecruitsClaim c : ClientManager.recruitsClaims) if (c.containsChunk(pos)) return false;
            RecruitsClaim neighbor = getNeighborClaim(pos);
            if (neighbor == null || neighbor.getClaimedChunks().size() >= RecruitsClaim.MAX_SIZE) return false;
            return !isInBufferZone(pos, ClientManager.ownFaction);
        }

        public boolean canClaimArea(List < ChunkPos > areaChunks) {
            if (selectedChunk == null || areaChunks == null || areaChunks.isEmpty() || ClientManager.ownFaction == null)
                return false;
            if (isPlayerTooFar(selectedChunk)) return false;
            for (ChunkPos chunk : areaChunks) {
                for (RecruitsClaim c : ClientManager.recruitsClaims) if (c.containsChunk(chunk)) return false;
                if (isInBufferZone(chunk, ClientManager.ownFaction)) return false;
            }
            return true;
        }

        public List<ChunkPos> getClaimableChunks(ChunkPos center,int radius){
            List<ChunkPos> result = new ArrayList<>();
            if (center == null || ClientManager.ownFaction == null) return result;
            for (int x = center.x - radius; x <= center.x + radius; x++)
                for (int z = center.z - radius; z <= center.z + radius; z++) {
                    ChunkPos chunk = new ChunkPos(x, z);
                    if (canClaimChunkRaw(chunk)) result.add(chunk);
                }
            return result;
        }

        public boolean canClaimChunkRaw(ChunkPos pos){
            for (RecruitsClaim c : ClientManager.recruitsClaims) if (c.containsChunk(pos)) return false;
            RecruitsClaim neighbor = getNeighborClaim(pos);
            if (neighbor == null) return false;
            return !isInBufferZone(pos, ClientManager.ownFaction);
        }
    }

