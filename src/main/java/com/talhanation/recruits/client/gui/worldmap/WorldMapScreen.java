package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;


public class WorldMapScreen extends Screen {
    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
    private final ChunkTileManager tileManager;
    private final Player player;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 6.0;
    private static final double DEFAULT_SCALE = 2.0;
    private static final double SCALE_STEP = 0.1;
    private static final int CHUNK_HIGHLIGHT_COLOR = 0x40FFFFFF;
    private static final int CHUNK_SELECTION_COLOR = 0xFFFFFFFF;
    private static final int DARK_GRAY_BG = 0xFF101010;
    private double offsetX = 0, offsetZ = 0;
    public static double scale = DEFAULT_SCALE;
    public double lastMouseX, lastMouseY;
    private boolean isDragging = false;
    private ChunkPos hoveredChunk = null;
    private ChunkPos selectedChunk = null;
    private int hoveredBlockX = 0, hoveredBlockZ = 0;

    private final WorldMapContextMenu contextMenu;
    public WorldMapScreen() {
        super(Component.literal(""));
        this.contextMenu =  new WorldMapContextMenu(this);
        this.tileManager = ChunkTileManager.getInstance();
        this.player = Minecraft.getInstance().player;
    }

    public ChunkPos getHoveredChunk() { return hoveredChunk; }
    public ChunkPos getSelectedChunk() { return selectedChunk; }
    public Player getPlayer() { return player; }
    public double getScale() { return scale; }
    public void setSelectedChunk(ChunkPos chunk) { this.selectedChunk = chunk; }

    @Override
    protected void init() {
        super.init();

        if (minecraft.level != null && player != null) {
            tileManager.initialize(minecraft.level);
            centerOnPlayer();
        }
    }

    public void centerOnPlayer() {
        if (player != null) {
            int chunkX = player.chunkPosition().x;
            int chunkZ = player.chunkPosition().z;
            double pixelX = chunkX * 16 * scale;
            double pixelZ = chunkZ * 16 * scale;
            offsetX = -pixelX + width / 2.0;
            offsetZ = -pixelZ + height / 2.0;
        }
    }

    public void resetZoom() {
        scale = DEFAULT_SCALE;
        centerOnPlayer();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);

        guiGraphics.enableScissor(0, 0, width, height);

        renderMapTiles(guiGraphics);

        if (player != null) {
            renderPlayerIcon(guiGraphics);
        }

        if (selectedChunk != null) {
            renderChunkOutline(guiGraphics, selectedChunk.x, selectedChunk.z, CHUNK_SELECTION_COLOR);
        }

        if (hoveredChunk != null) {
            renderChunkHighlight(guiGraphics, hoveredChunk.x, hoveredChunk.z);
        }

        guiGraphics.disableScissor();

        renderCoordinatesAndZoom(guiGraphics);

        renderFPS(guiGraphics);

        contextMenu.render(guiGraphics, this);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, DARK_GRAY_BG);
    }

    private void renderMapTiles(GuiGraphics guiGraphics) {
        double minTileX = (-offsetX) / (ChunkTile.TILE_PIXEL_SIZE * scale);
        double maxTileX = (width - offsetX) / (ChunkTile.TILE_PIXEL_SIZE * scale);
        double minTileZ = (-offsetZ) / (ChunkTile.TILE_PIXEL_SIZE * scale);
        double maxTileZ = (height - offsetZ) / (ChunkTile.TILE_PIXEL_SIZE * scale);

        int startTileX = (int)Math.floor(minTileX) - 1;
        int endTileX = (int)Math.ceil(maxTileX) + 1;
        int startTileZ = (int)Math.floor(minTileZ) - 1;
        int endTileZ = (int)Math.ceil(maxTileZ) + 1;

        for (int tileZ = startTileZ; tileZ <= endTileZ; tileZ++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                ChunkTile tile = tileManager.getOrCreateTile(tileX, tileZ);
                int x = (int)(offsetX + tileX * ChunkTile.TILE_PIXEL_SIZE * scale);
                int z = (int)(offsetZ + tileZ * ChunkTile.TILE_PIXEL_SIZE * scale);
                int size = (int)(ChunkTile.TILE_PIXEL_SIZE * scale);
                tile.render(guiGraphics, x, z, size);
            }
        }
    }

    private void renderPlayerIcon(GuiGraphics guiGraphics) {
        if (player == null) return;

        double playerWorldX = player.getX();
        double playerWorldZ = player.getZ();

        int pixelX = (int)(offsetX + playerWorldX * scale);
        int pixelZ = (int)(offsetZ + playerWorldZ * scale);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        pose.translate(pixelX, pixelZ, 0);

        pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot()));

        float iconScale = (float)Math.max(2.0, 5.0 * scale / 2.0);
        pose.scale(iconScale, iconScale, 1.0f);


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

        consumer.vertex(matrix, -1f, 1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();

        consumer.vertex(matrix, 1f, 1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();

        consumer.vertex(matrix, 1f, -1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();

        consumer.vertex(matrix, -1f, -1f, 0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 0, 1)
                .endVertex();

        pose.popPose();

        renderPlayerNameTag(guiGraphics, pixelX, pixelZ);
    }
    private void renderPlayerNameTag(GuiGraphics guiGraphics, int pixelX, int pixelZ) {
        if (player != null && scale > 1.5) {
            String playerName = player.getName().getString();
            int nameWidth = font.width(playerName);

            guiGraphics.fill(pixelX - nameWidth/2 - 2, pixelZ - 15, pixelX + nameWidth/2 + 2, pixelZ - 5, 0x80000000);

            guiGraphics.drawCenteredString(font, playerName, pixelX, pixelZ - 15, 0xFFFFFF);
        }
    }

    private void renderChunkHighlight(GuiGraphics guiGraphics, int chunkX, int chunkZ) {
        int pixelX = (int)(offsetX + chunkX * 16 * scale);
        int pixelZ = (int)(offsetZ + chunkZ * 16 * scale);
        int size = (int)(16 * scale);
        guiGraphics.fill(pixelX, pixelZ, pixelX + size, pixelZ + size, CHUNK_HIGHLIGHT_COLOR);
    }

    private void renderChunkOutline(GuiGraphics guiGraphics, int chunkX, int chunkZ, int color) {
        int pixelX = (int)(offsetX + chunkX * 16 * scale);
        int pixelZ = (int)(offsetZ + chunkZ * 16 * scale);
        int size = (int)(16 * scale);

        guiGraphics.hLine(pixelX, pixelX + size, pixelZ, color);
        guiGraphics.hLine(pixelX, pixelX + size, pixelZ + size, color);
        guiGraphics.vLine(pixelX, pixelZ, pixelZ + size, color);
        guiGraphics.vLine(pixelX + size, pixelZ, pixelZ + size, color);
    }

    private void renderCoordinatesAndZoom(GuiGraphics guiGraphics) {
        String coords = String.format("X: %d, Z: %d", hoveredBlockX, hoveredBlockZ);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu.isVisible()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button, this)) {
                return true;
            }
        }

        if (button == 1) {
            contextMenu.openAt((int)mouseX, (int)mouseY, this);
            return true;
        }

        if (button == 0 && !contextMenu.isVisible()) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            isDragging = true;

            if (hoveredChunk != null) {
                selectedChunk = hoveredChunk;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (contextMenu.isVisible()) {
            return false;
        }

        if (button == 0) {
            isDragging = false;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            offsetX += mouseX - lastMouseX;
            offsetZ += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        if (contextMenu.isVisible()) {
            contextMenu.close();
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contextMenu.isVisible()) {
            contextMenu.close();
        }

        double zoomFactor = 1.0 + (delta > 0 ? SCALE_STEP : -SCALE_STEP);
        double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * zoomFactor));

        double mouseWorldX = (mouseX - offsetX) / scale;
        double mouseWorldZ = (mouseY - offsetZ) / scale;
        scale = newScale;
        offsetX = mouseX - mouseWorldX * scale;
        offsetZ = mouseY - mouseWorldZ * scale;

        return true;
    }
    public double mouseX, mouseY;
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        double worldX = (mouseX - offsetX) / scale;
        double worldZ = (mouseY - offsetZ) / scale;
        hoveredBlockX = (int)Math.floor(worldX);
        hoveredBlockZ = (int)Math.floor(worldZ);
        hoveredChunk = new ChunkPos(hoveredBlockX >> 4, hoveredBlockZ >> 4);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (contextMenu.isVisible()) {
                contextMenu.close();
                return true;
            }
            onClose();
            return true;
        }

        if (!contextMenu.isVisible()) {
            double moveSpeed = 40.0 / scale;
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> offsetZ += moveSpeed;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> offsetZ -= moveSpeed;
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> offsetX += moveSpeed;
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> offsetX -= moveSpeed;
                case GLFW.GLFW_KEY_EQUAL -> mouseScrolled(width/2.0, height/2.0, 1);
                case GLFW.GLFW_KEY_MINUS -> mouseScrolled(width/2.0, height/2.0, -1);
                case GLFW.GLFW_KEY_C -> centerOnPlayer();
                case GLFW.GLFW_KEY_R -> resetZoom();
            }
        }

        return true;
    }

    @Override
    public void onClose() {
        tileManager.close();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    private long lastFpsTime = 0;
    private int fpsCounter = 0;
    private int currentFps = 0;
    private void renderFPS(GuiGraphics guiGraphics) {
        long currentTime = System.currentTimeMillis();
        fpsCounter++;

        if (currentTime - lastFpsTime >= 1000) {
            currentFps = fpsCounter;
            fpsCounter = 0;
            lastFpsTime = currentTime;
        }

        String fpsText = String.format("FPS: %d", currentFps);
        guiGraphics.drawString(font, fpsText, width - font.width(fpsText) - 15, 5, 0x00FF00);
    }
}