package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.compat.SmallShips;
import com.talhanation.recruits.network.MessageDoPayment;
import com.talhanation.recruits.network.MessageUpdateClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
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
    private final ChunkTileManager tileManager;
    private final Player player;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 10.0;
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
    ChunkPos selectedChunk = null;
    private int clickedBlockX = 0, clickedBlockZ = 0;
    private int hoverBlockX = 0, hoverBlockZ = 0;
    private WorldMapContextMenu contextMenu;
    RecruitsClaim selectedClaim = null;
    private ClaimInfoMenu claimInfoMenu;

    public WorldMapScreen() {
        super(Component.literal(""));
        this.contextMenu = new WorldMapContextMenu(this);
        this.claimInfoMenu = new ClaimInfoMenu(this);
        this.tileManager = ChunkTileManager.getInstance();
        this.player = Minecraft.getInstance().player;
    }

    public BlockPos getHoveredBlockPos() { return new BlockPos(hoverBlockX, 0, hoverBlockZ); }
    public BlockPos getClickedBlockPos() { return new BlockPos(clickedBlockX, 0, clickedBlockZ); }
    public Player getPlayer() { return player; }
    public boolean isPlayerAdminAndCreative() { return player.hasPermissions(2) && player.isCreative(); }
    public double getScale() { return scale; }
    public void setSelectedChunk(ChunkPos chunk) { this.selectedChunk = chunk; }

    @Override
    protected void init() {
        super.init();

        if (minecraft.level != null && player != null) {
            tileManager.initialize(minecraft.level);
            centerOnPlayer();
        }

        claimInfoMenu.init();
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

        ClaimRenderer.renderClaimsOverlay(guiGraphics, this.selectedClaim, this.offsetX, this.offsetZ, scale);

        if (contextMenu.isVisible()) {
            String entryTag = contextMenu.getHoveredEntryTag();
            if (entryTag != null){
                if(entryTag.contains("bufferzone")) {
                    ClaimRenderer.renderBufferZone(guiGraphics, offsetX, offsetZ, scale);
                }
                if(entryTag.contains("area")){
                    ClaimRenderer.renderAreaPreview(guiGraphics, getClaimArea(selectedChunk) , offsetX, offsetZ, scale);
                }
                if(entryTag.contains("chunk")){
                    ClaimRenderer.renderAreaPreview(guiGraphics, getClaimableChunks(selectedChunk, 16) , offsetX, offsetZ, scale);
                }
            }
        }

        if (player != null) {
            renderPlayerPosition(guiGraphics);
        }

         if (selectedChunk != null && (selectedClaim == null || contextMenu.isVisible())) {
            renderChunkOutline(guiGraphics, selectedChunk.x, selectedChunk.z, CHUNK_SELECTION_COLOR);
        }

        if (hoveredChunk != null) {
            renderChunkHighlight(guiGraphics, hoveredChunk.x, hoveredChunk.z);
        }

        guiGraphics.disableScissor();

        renderCoordinatesAndZoom(guiGraphics);

        //renderFPS(guiGraphics);

        contextMenu.render(guiGraphics, this);

        if (selectedClaim != null && claimInfoMenu.isVisible()) {
            Point p = getClaimInfoMenuPosition(selectedClaim, claimInfoMenu.width, claimInfoMenu.height
            );
            claimInfoMenu.setPosition(p.x, p.y);
            claimInfoMenu.render(guiGraphics);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, DARK_GRAY_BG);
    }

    private void renderMapTiles(GuiGraphics guiGraphics) {
        double tileSize = ChunkTile.TILE_PIXEL_SIZE;
        double scaledTileSize = tileSize * scale;

        double leftEdge = -offsetX;
        double rightEdge = width - offsetX;
        double topEdge = -offsetZ;
        double bottomEdge = height - offsetZ;

        int startTileX = (int)Math.floor(leftEdge / scaledTileSize - 0.5);
        int endTileX = (int)Math.ceil(rightEdge / scaledTileSize + 0.5);
        int startTileZ = (int)Math.floor(topEdge / scaledTileSize - 0.5);
        int endTileZ = (int)Math.ceil(bottomEdge / scaledTileSize + 0.5);


        for (int tileZ = startTileZ; tileZ <= endTileZ; tileZ++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                ChunkTile tile = tileManager.getOrCreateTile(tileX, tileZ);
                ResourceLocation textureId = tile.getTextureId();

                if (textureId == null) continue;


                double tileWorldX = tileX * scaledTileSize + offsetX;
                double tileWorldZ = tileZ * scaledTileSize + offsetZ;

                double drawX = tileWorldX - 0.5;
                double drawZ = tileWorldZ - 0.5;
                double drawSize = scaledTileSize + 1.0;


                int x = (int)Math.floor(drawX);
                int z = (int)Math.floor(drawZ);
                int size = (int)Math.ceil(drawSize);

                if (Math.abs(scale - 1.0) < 0.01) {
                    x = (int)Math.round(tileWorldX);
                    z = (int)Math.round(tileWorldZ);
                    size = (int)Math.round(scaledTileSize);
                }

                RenderSystem.setShaderTexture(0, textureId);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                guiGraphics.blit(textureId, x, z, 0, 0, size, size, size, size);
            }
        }
    }

    private static final ItemStack BOAT_STACK = new ItemStack(Items.OAK_BOAT);
    private void renderPlayerPosition(GuiGraphics guiGraphics) {
        if (player == null) return;

        double playerWorldX = player.getX();
        double playerWorldZ = player.getZ();

        int pixelX = (int)(offsetX + playerWorldX * scale);
        int pixelZ = (int)(offsetZ + playerWorldZ * scale);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        pose.translate(pixelX, pixelZ, 0);

        if(player.getVehicle() instanceof Boat){
            renderPlayerBoat(pose, guiGraphics);
        }
        else{
            renderPlayerIcon(pose, guiGraphics);
        }

        pose.popPose();

        renderPlayerNameTag(guiGraphics, pixelX, pixelZ);
    }

    private void renderPlayerBoat(PoseStack pose, GuiGraphics guiGraphics){
        float yaw = player.getYRot() % 360f;
        if (yaw < -180f) yaw += 360f;
        if (yaw >= 180f) yaw -= 360f;

        boolean flipX = yaw > 0;

        pose.pushPose();

        if (flipX) {
            pose.scale(-1f, 1f, 1f);
        }

        pose.scale(1.5f, 1.5f, 1.5f);

        Lighting.setupForFlatItems();

        ItemStack boat = BOAT_STACK;
        if(Main.isSmallShipsLoaded ){
            if(player.getVehicle() != null && SmallShips.isSmallShip(player.getVehicle())){
                 boat = SmallShips.getSmallShipsItem();
            }
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

    }

    private void renderPlayerNameTag(GuiGraphics guiGraphics, int pixelX, int pixelZ) {
        if (player != null && scale > 1.5) {
            String playerName = player.getName().getString();
            float textScale = (float)Math.min(1.0, scale / 1.25);
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
        String coords = String.format("X: %d, Z: %d", hoverBlockX, hoverBlockZ);
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

    public void centerOnClaim(RecruitsClaim claim) {
        if (claim == null || claim.getCenter() == null) return;

        ChunkPos center = claim.getCenter();
        double pixelX = center.x * 16 * scale;
        double pixelZ = center.z * 16 * scale;

        offsetX = -pixelX + width / 2.0;
        offsetZ = -pixelZ + height / 2.0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check Claim Info Menu first
        if (claimInfoMenu.isVisible() && claimInfoMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (contextMenu.isVisible()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button, this)) {
                return true;
            }
        }
        if(hoveredChunk != null)  selectedChunk = hoveredChunk;

        RecruitsClaim clickedClaim = ClaimRenderer.getClaimAtPosition(mouseX, mouseY, offsetX, offsetZ, scale);
        if (clickedClaim != null) {
            selectedClaim = clickedClaim;
            claimInfoMenu.openForClaim(selectedClaim, (int) mouseX, (int) mouseY);
        }
        else {
            selectedClaim = null;
            claimInfoMenu.close();
        }

        if (button == 1) { // Rechtsklick
            double worldX = (mouseX - offsetX) / scale;
            double worldZ = (mouseY - offsetZ) / scale;
            clickedBlockX = (int) Math.floor(worldX);
            clickedBlockZ = (int) Math.floor(worldZ);

            this.contextMenu = new WorldMapContextMenu(this);
            contextMenu.openAt((int) mouseX, (int) mouseY);
            claimInfoMenu.close();
        }

        if (button == 0) { // Linksklick
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            isDragging = true;
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

        if (claimInfoMenu.isVisible()) {
            claimInfoMenu.mouseReleased(mouseX, mouseY, button);
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

            if (claimInfoMenu.isVisible()) {
                claimInfoMenu.close();
            }
            return true;
        }

        if (claimInfoMenu.isVisible()) {
            claimInfoMenu.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (claimInfoMenu.isVisible()) {
            claimInfoMenu.close();
        }

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
        int worldX = (int) ((mouseX - offsetX) / scale);
        int worldZ = (int) ((mouseY - offsetZ) / scale);
        hoverBlockX = (int)Math.floor(worldX);
        hoverBlockZ = (int)Math.floor(worldZ);
        hoveredChunk = new ChunkPos(hoverBlockX >> 4, hoverBlockZ >> 4);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

        // Navigationstasten
        if (!contextMenu.isVisible() && !claimInfoMenu.isVisible()) {
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
    public boolean isPlayerFactionLeader() {
        return this.isPlayerFactionLeader(ownFaction);
    }
    public boolean isPlayerFactionLeader(RecruitsFaction faction) {
        if(player == null || faction ==  null) return false;

        return faction.getTeamLeaderUUID().equals(player.getUUID());
    }

    public boolean isPlayerClaimLeader(){
        return this.isPlayerClaimLeader(selectedClaim);
    }
    public boolean isPlayerClaimLeader(RecruitsClaim claim) {
        if(player == null || claim ==  null) return false;

        return claim.getPlayerInfo().getUUID().equals(player.getUUID());
    }
    public List<ChunkPos> getClaimArea(ChunkPos pos){
        List<ChunkPos> area = new ArrayList<>();
        if(pos == null) return area;

        int range = 2;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                area.add(new ChunkPos(pos.x + dx, pos.z + dz));
            }
        }

        return area;
    }
    public void claimArea() {
        if(!canPlayerPay(getClaimCost(ownFaction), player)) return;
        if(!ClientManager.configValueIsClaimingAllowed) return;

        List<ChunkPos> area = getClaimArea(this.selectedChunk);

        RecruitsClaim newClaim = new RecruitsClaim(ownFaction);
        for (ChunkPos pos : area) {
            newClaim.addChunk(pos);
        }

        newClaim.setCenter(selectedChunk);
        newClaim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownFaction));

        Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), getClaimCost(ownFaction)));

        ClientManager.recruitsClaims.add(newClaim);
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
    }

    public void claimChunk() {
        if(!canPlayerPay(ClientManager.configValueChunkCost, player)) return;
        if(!ClientManager.configValueIsClaimingAllowed) return;

        RecruitsClaim neighborClaim = getNeighborClaim(selectedChunk);
        if(neighborClaim == null) return;

        String ownerID = ownFaction.getStringID();
        String neighborID = neighborClaim.getOwnerFaction().getStringID();
        if(!Objects.equals(ownerID, neighborID)) return;

        for(RecruitsClaim claim : ClientManager.recruitsClaims){
            if(claim.equals(neighborClaim)){
                neighborClaim.addChunk(selectedChunk);
                this.recalculateCenter(neighborClaim);
                break;
            }
        }
        Main.SIMPLE_CHANNEL.sendToServer(new MessageDoPayment(player.getUUID(), ClientManager.configValueChunkCost));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(neighborClaim));
    }

    @Nullable
    public RecruitsClaim getNeighborClaim(ChunkPos chunk) {
        ChunkPos[] neighbors = new ChunkPos[] {
                new ChunkPos(chunk.x + 1, chunk.z),
                new ChunkPos(chunk.x - 1, chunk.z),
                new ChunkPos(chunk.x, chunk.z + 1),
                new ChunkPos(chunk.x, chunk.z - 1)
        };

        for (ChunkPos neighbor : neighbors) {
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim.containsChunk(neighbor)) {
                    return claim;
                }
            }
        }

        return null;
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

    public Rectangle getClaimScreenBounds(RecruitsClaim claim) {
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;

        for (ChunkPos pos : claim.getClaimedChunks()) {
            minChunkX = Math.min(minChunkX, pos.x);
            maxChunkX = Math.max(maxChunkX, pos.x);
            minChunkZ = Math.min(minChunkZ, pos.z);
            maxChunkZ = Math.max(maxChunkZ, pos.z);
        }

        // World → Screen
        int x1 = (int) (offsetX + minChunkX * 16 * scale);
        int y1 = (int) (offsetZ + minChunkZ * 16 * scale);
        int x2 = (int) (offsetX + (maxChunkX + 1) * 16 * scale);
        int y2 = (int) (offsetZ + (maxChunkZ + 1) * 16 * scale);

        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    public Point getClaimInfoMenuPosition(RecruitsClaim claim, int menuWidth, int menuHeight) {
        Rectangle bounds = getClaimScreenBounds(claim);

        int margin = 10;

        int x = bounds.x + bounds.width + margin;
        int y = bounds.y + bounds.height / 2 - menuHeight / 2;

        // Bildschirm-Clamping
        if (x + menuWidth > width) {
            x = bounds.x - menuWidth - margin; // links anzeigen
        }
        if (y < 10) y = 10;
        if (y + menuHeight > height - 10) {
            y = height - menuHeight - 10;
        }

        return new Point(x, y);
    }

    public boolean canRemoveChunk(ChunkPos pos, RecruitsClaim claim) {
        if (pos == null || ownFaction == null) return false;
        if (isPlayerTooFar(pos)) return false;

        List<ChunkPos> claimedChunks = claim.getClaimedChunks();
        if (!claimedChunks.contains(pos)) return false;

        int unclaimedNeighborCount = 0;
        ChunkPos[] neighbors = new ChunkPos[] {
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1),
                new ChunkPos(pos.x, pos.z - 1)
        };

        for (ChunkPos neighbor : neighbors) {
            if (!claimedChunks.contains(neighbor)) {
                unclaimedNeighborCount++;
            }
        }

        return unclaimedNeighborCount >= 2;
    }

    private boolean isPlayerTooFar(ChunkPos pos) {
        if(pos == null) return true;
        int playerPosX = player.chunkPosition().x;
        int playerPosZ = player.chunkPosition().z;

        int diffX = Math.abs(playerPosX) - Math.abs(pos.x);
        int diffZ = Math.abs(playerPosZ) - Math.abs(pos.z);

        return Math.abs(diffZ) > 4 || Math.abs(diffX) > 4;
    }

    public int getClaimCost(RecruitsFaction ownerTeam) {
        if (!ClientManager.configValueCascadeClaimCost) {
            return ClientManager.configValueClaimCost;
        }

        int amount = 1;
        if(ownerTeam != null){
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim.getOwnerFaction().getStringID().equals(ownerTeam.getStringID())) {
                    amount += 1;
                }
            }
        }

        return amount * ClientManager.configValueClaimCost;
    }

    public boolean canPlayerPay(int cost, Player player){
        return player.isCreative() || cost <= player.getInventory().countItem(ClientManager.currencyItemStack.getItem());
    }

    public static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction) {
        if (ownFaction == null) return false;

        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.getOwnerFaction() == null || claim.getOwnerFaction().getStringID().equals(ownFaction.getStringID())) {
                continue;
            }

            for (ChunkPos claimChunk : claim.getClaimedChunks()) {


                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);

                if (dx <= 3 && dz <= 3) {
                    if (!(dx == 0 && dz == 0)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    public boolean canClaimChunk(ChunkPos pos) {
        if (!ClientManager.configValueIsClaimingAllowed) return false;
        if (pos == null || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(pos)) return false;

        for (RecruitsClaim existingClaim : ClientManager.recruitsClaims) {
            if (existingClaim.containsChunk(pos)) {
                return false;
            }
        }

        RecruitsClaim neighborClaim = getNeighborClaim(pos);
        if (neighborClaim == null) return false;

        if(neighborClaim.getClaimedChunks().size() >= RecruitsClaim.MAX_SIZE) return false;

        return !isInBufferZone(pos, ClientManager.ownFaction);
    }
    public boolean canClaimArea(List<ChunkPos> areaChunks) {
        if (selectedChunk == null || areaChunks == null || areaChunks.isEmpty() || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(selectedChunk)) return false;

        //if(ownFaction.claimAmount >= ownFaction.getMaxClaims()) return false;

        for (ChunkPos chunk : areaChunks) {
            for (RecruitsClaim existingClaim : ClientManager.recruitsClaims) {
                if (existingClaim.containsChunk(chunk)) {
                    return false;
                }
            }

            if (isInBufferZone(chunk, ClientManager.ownFaction)) {
                return false;
            }
        }

        return true;
    }

    public List<ChunkPos> getClaimableChunks(ChunkPos center, int radius) {
        List<ChunkPos> claimableChunks = new ArrayList<>();

        if (center == null || ClientManager.ownFaction == null) return claimableChunks;

        int minX = center.x - radius;
        int maxX = center.x + radius;
        int minZ = center.z - radius;
        int maxZ = center.z + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos chunk = new ChunkPos(x, z);

                if (canClaimChunkRaw(chunk)) {
                    claimableChunks.add(chunk);
                }
            }
        }

        return claimableChunks;
    }

    public boolean canClaimChunkRaw(ChunkPos pos) {
        for (RecruitsClaim existingClaim : ClientManager.recruitsClaims) {
            if (existingClaim.containsChunk(pos)) {
                return false;
            }
        }

        RecruitsClaim neighborClaim = getNeighborClaim(pos);
        if (neighborClaim == null) return false;

        return !isInBufferZone(pos, ClientManager.ownFaction);
    }

}