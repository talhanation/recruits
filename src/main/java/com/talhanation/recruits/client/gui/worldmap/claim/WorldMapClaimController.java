package com.talhanation.recruits.client.gui.worldmap.claim;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.WorldMapCamera;
import com.talhanation.recruits.network.MessageDoPayment;
import com.talhanation.recruits.network.MessageUpdateClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WorldMapClaimController {
    private static final int CLAIM_AREA_RANGE = 2;
    private static final int MAX_PLAYER_CLAIM_DISTANCE_CHUNKS = 4;

    private final Minecraft minecraft;
    private final Player player;
    private final WorldMapCamera camera;

    public WorldMapClaimController(Minecraft minecraft, Player player, WorldMapCamera camera) {
        this.minecraft = minecraft;
        this.player = player;
        this.camera = camera;
    }

    public boolean isPlayerFactionLeader() {
        return isPlayerFactionLeader(ClientManager.ownFaction);
    }

    public boolean isPlayerFactionLeader(RecruitsFaction faction) {
        if (player == null || faction == null) return false;
        return Objects.equals(faction.getTeamLeaderUUID(), player.getUUID());
    }

    public boolean isPlayerClaimLeader(RecruitsClaim claim) {
        if (player == null || claim == null || claim.getPlayerInfo() == null) return false;
        return Objects.equals(claim.getPlayerInfo().getUUID(), player.getUUID());
    }

    public List<ChunkPos> getClaimArea(ChunkPos pos) {
        List<ChunkPos> area = new ArrayList<>();
        if (pos == null) return area;
        for (int dx = -CLAIM_AREA_RANGE; dx <= CLAIM_AREA_RANGE; dx++) {
            for (int dz = -CLAIM_AREA_RANGE; dz <= CLAIM_AREA_RANGE; dz++) {
                area.add(new ChunkPos(pos.x + dx, pos.z + dz));
            }
        }
        return area;
    }

    public void claimArea(ChunkPos selectedChunk) {
        if (!canExecuteClaimArea(selectedChunk)) return;

        List<ChunkPos> area = getClaimArea(selectedChunk);
        RecruitsClaim newClaim = new RecruitsClaim(ClientManager.ownFaction);

        for (ChunkPos pos : area) newClaim.addChunk(pos);
        newClaim.setCenter(selectedChunk);
        newClaim.setPlayer(
                new RecruitsPlayerInfo(
                        player.getUUID(), player.getName().getString(), ClientManager.ownFaction));
        Main.SIMPLE_CHANNEL.sendToServer(
                new MessageDoPayment(player.getUUID(), getClaimCost(ClientManager.ownFaction)));
        ClientManager.recruitsClaims.add(newClaim);
        WorldMapClaimIndex.invalidate();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
    }

    public void claimChunk(ChunkPos selectedChunk) {
        if (!canExecuteClaimChunk(selectedChunk)) return;

        RecruitsClaim neighborClaim = getNeighborClaim(selectedChunk);
        if (neighborClaim == null) return;

        neighborClaim.addChunk(selectedChunk);
        recalculateCenter(neighborClaim);
        WorldMapClaimIndex.invalidate();

        Main.SIMPLE_CHANNEL.sendToServer(
                new MessageDoPayment(player.getUUID(), ClientManager.configValueChunkCost));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(neighborClaim));
    }

    @Nullable
    public RecruitsClaim getNeighborClaim(ChunkPos chunk) {
        return WorldMapClaimIndex.getNeighborClaim(chunk);
    }

    public void recalculateCenter(RecruitsClaim claim) {
        if (claim == null || claim.getClaimedChunks() == null) return;

        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkPos pos : chunks) {
            if (pos == null) continue;
            if (pos.x < minX) minX = pos.x;
            if (pos.x > maxX) maxX = pos.x;
            if (pos.z < minZ) minZ = pos.z;
            if (pos.z > maxZ) maxZ = pos.z;
        }
        if (minX == Integer.MAX_VALUE) return;
        claim.setCenter(new ChunkPos((minX + maxX) / 2, (minZ + maxZ) / 2));
    }

    public void centerOnClaim(RecruitsClaim claim) {
        if (claim == null || claim.getCenter() == null) return;
        camera.centerOnClaim(claim.getCenter());
    }

    public Rectangle getClaimScreenBounds(
            RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
        if (claim == null || claim.getClaimedChunks() == null || claim.getClaimedChunks().isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkPos pos : claim.getClaimedChunks()) {
            if (pos == null) continue;
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minZ = Math.min(minZ, pos.z);
            maxZ = Math.max(maxZ, pos.z);
        }
        if (minX == Integer.MAX_VALUE) return new Rectangle(0, 0, 0, 0);

        int x1 = (int) (offsetX + minX * 16 * scale);
        int y1 = (int) (offsetZ + minZ * 16 * scale);
        int x2 = (int) (offsetX + (maxX + 1) * 16 * scale);
        int y2 = (int) (offsetZ + (maxZ + 1) * 16 * scale);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    public Point getClaimInfoMenuPosition(
            RecruitsClaim claim,
            int menuWidth,
            int menuHeight,
            int screenWidth,
            int screenHeight,
            double offsetX,
            double offsetZ,
            double scale) {
        Rectangle bounds = getClaimScreenBounds(claim, offsetX, offsetZ, scale);
        int margin = 10;
        int x = bounds.x + bounds.width + margin;
        int y = bounds.y + bounds.height / 2 - menuHeight / 2;
        if (x + menuWidth > screenWidth) x = bounds.x - menuWidth - margin;
        if (y < 10) y = 10;
        if (y + menuHeight > screenHeight - 10) y = screenHeight - menuHeight - 10;
        return new Point(x, y);
    }

    public boolean canRemoveChunk(ChunkPos pos, RecruitsClaim claim) {
        if (pos == null || claim == null || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(pos)) return false;
        List<ChunkPos> claimedChunks = claim.getClaimedChunks();
        if (claimedChunks == null || !claimedChunks.contains(pos)) return false;
        int unclaimedNeighbors = 0;
        for (ChunkPos neighbor :
                new ChunkPos[] {
                    new ChunkPos(pos.x + 1, pos.z), new ChunkPos(pos.x - 1, pos.z),
                    new ChunkPos(pos.x, pos.z + 1), new ChunkPos(pos.x, pos.z - 1)
                }) {
            if (!claimedChunks.contains(neighbor)) unclaimedNeighbors++;
        }
        return unclaimedNeighbors >= 2;
    }

    public int getClaimCost(RecruitsFaction ownerTeam) {
        if (!ClientManager.configValueCascadeClaimCost) return ClientManager.configValueClaimCost;
        int amount = 1;
        if (ownerTeam != null) {
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim == null || claim.getOwnerFaction() == null) continue;
                if (sameFaction(claim.getOwnerFaction(), ownerTeam)) amount++;
            }
        }
        return amount * ClientManager.configValueClaimCost;
    }

    public boolean canPlayerPay(int cost, Player player) {
        if (player == null) return false;
        if (player.isCreative()) return true;

        ItemStack currency = ClientManager.currencyItemStack;
        return currency != null
                && !currency.isEmpty()
                && cost <= player.getInventory().countItem(currency.getItem());
    }

    public static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction) {
        if (chunk == null || ownFaction == null) return false;
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim == null || claim.getOwnerFaction() == null || claim.getClaimedChunks() == null) continue;
            if (sameFaction(claim.getOwnerFaction(), ownFaction)) continue;
            for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                if (claimChunk == null) continue;
                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);
                if (dx <= 3 && dz <= 3 && !(dx == 0 && dz == 0)) return true;
            }
        }
        return false;
    }

    public boolean canClaimChunk(ChunkPos pos) {
        return getClaimChunkStatus(pos, true) == ClaimPreviewStatus.VALID;
    }

    public boolean canShowClaimChunkEntry(ChunkPos pos) {
        return pos != null && ClientManager.ownFaction != null;
    }

    public boolean canExecuteClaimChunk(ChunkPos pos) {
        if (getClaimChunkStatus(pos, true) != ClaimPreviewStatus.VALID) return false;
        if (!canPlayerPay(ClientManager.configValueChunkCost, player)) return false;

        RecruitsClaim neighborClaim = getNeighborClaim(pos);
        return isPlayerFactionLeader() || isPlayerClaimLeader(neighborClaim);
    }

    public Component getClaimChunkDisabledReason(ChunkPos pos) {
        ClaimPreviewStatus status = getClaimChunkStatus(pos, true);
        if (status != ClaimPreviewStatus.VALID) return status.message();
        if (!canPlayerPay(ClientManager.configValueChunkCost, player)) {
            return ClaimPreviewStatus.NOT_ENOUGH_PAYMENT.message();
        }

        RecruitsClaim neighborClaim = getNeighborClaim(pos);
        if (!isPlayerFactionLeader() && !isPlayerClaimLeader(neighborClaim)) {
            return ClaimPreviewStatus.NO_PERMISSION.message();
        }
        return ClaimPreviewStatus.VALID.message();
    }

    public boolean canShowClaimAreaEntry(ChunkPos selectedChunk) {
        return selectedChunk != null && ClientManager.ownFaction != null;
    }

    public boolean canExecuteClaimArea(ChunkPos selectedChunk) {
        List<ChunkPos> areaChunks = getClaimArea(selectedChunk);
        if (getClaimAreaStatus(selectedChunk, areaChunks) != ClaimPreviewStatus.VALID) return false;
        if (!isPlayerFactionLeader()) return false;
        return canPlayerPay(getClaimCost(ClientManager.ownFaction), player);
    }

    public Component getClaimAreaDisabledReason(ChunkPos selectedChunk) {
        List<ChunkPos> areaChunks = getClaimArea(selectedChunk);
        ClaimPreviewStatus status = getClaimAreaStatus(selectedChunk, areaChunks);
        if (status != ClaimPreviewStatus.VALID) return status.message();
        if (!isPlayerFactionLeader()) return ClaimPreviewStatus.NO_PERMISSION.message();
        if (!canPlayerPay(getClaimCost(ClientManager.ownFaction), player)) {
            return ClaimPreviewStatus.NOT_ENOUGH_PAYMENT.message();
        }
        return ClaimPreviewStatus.VALID.message();
    }

    public ClaimPreviewStatus getClaimChunkStatus(ChunkPos pos, boolean requirePlayerDistance) {
        if (!ClientManager.configValueIsClaimingAllowed) return ClaimPreviewStatus.CLAIMING_DISABLED;
        if (pos == null) return ClaimPreviewStatus.NO_CHUNK;
        if (ClientManager.ownFaction == null) return ClaimPreviewStatus.NO_FACTION;
        if (!isPlayerInOverworld()) return ClaimPreviewStatus.WRONG_DIMENSION;
        if (requirePlayerDistance && isPlayerTooFar(pos)) return ClaimPreviewStatus.TOO_FAR;
        if (WorldMapClaimIndex.isClaimed(pos)) return ClaimPreviewStatus.OCCUPIED;

        RecruitsClaim neighbor = getNeighborClaim(pos);
        if (neighbor == null) return ClaimPreviewStatus.NO_NEIGHBOR;
        if (neighbor.getOwnerFaction() == null || !sameFaction(neighbor.getOwnerFaction(), ClientManager.ownFaction)) {
            return ClaimPreviewStatus.WRONG_FACTION;
        }
        if (neighbor.getClaimedChunks() == null || neighbor.getClaimedChunks().size() >= getMaxClaimChunks()) {
            return ClaimPreviewStatus.CLAIM_FULL;
        }
        if (isInBufferZone(pos, ClientManager.ownFaction)) return ClaimPreviewStatus.BUFFER_ZONE;
        return ClaimPreviewStatus.VALID;
    }

    public ClaimPreviewStatus getClaimAreaStatus(ChunkPos selectedChunk, List<ChunkPos> areaChunks) {
        if (!ClientManager.configValueIsClaimingAllowed) return ClaimPreviewStatus.CLAIMING_DISABLED;
        if (selectedChunk == null || areaChunks == null || areaChunks.isEmpty()) return ClaimPreviewStatus.NO_CHUNK;
        if (ClientManager.ownFaction == null) return ClaimPreviewStatus.NO_FACTION;
        if (!isPlayerInOverworld()) return ClaimPreviewStatus.WRONG_DIMENSION;
        if (isPlayerTooFar(selectedChunk)) return ClaimPreviewStatus.TOO_FAR;
        if (areaChunks.size() > getMaxClaimChunks()) return ClaimPreviewStatus.CLAIM_FULL;

        for (ChunkPos chunk : areaChunks) {
            ClaimPreviewStatus status = getClaimAreaChunkStatus(selectedChunk, chunk);
            if (status != ClaimPreviewStatus.VALID) return status;
        }
        return ClaimPreviewStatus.VALID;
    }

    public ClaimPreviewStatus getClaimAreaChunkStatus(ChunkPos selectedChunk, ChunkPos chunk) {
        if (!ClientManager.configValueIsClaimingAllowed) return ClaimPreviewStatus.CLAIMING_DISABLED;
        if (selectedChunk == null || chunk == null) return ClaimPreviewStatus.NO_CHUNK;
        if (ClientManager.ownFaction == null) return ClaimPreviewStatus.NO_FACTION;
        if (!isPlayerInOverworld()) return ClaimPreviewStatus.WRONG_DIMENSION;
        if (isPlayerTooFar(selectedChunk)) return ClaimPreviewStatus.TOO_FAR;
        if (WorldMapClaimIndex.isClaimed(chunk)) return ClaimPreviewStatus.OCCUPIED;
        if (isInBufferZone(chunk, ClientManager.ownFaction)) return ClaimPreviewStatus.BUFFER_ZONE;
        return ClaimPreviewStatus.VALID;
    }

    public List<ClaimPreviewChunk> getClaimAreaPreview(ChunkPos selectedChunk) {
        List<ClaimPreviewChunk> preview = new ArrayList<>();
        for (ChunkPos chunk : getClaimArea(selectedChunk)) {
            preview.add(new ClaimPreviewChunk(chunk, getClaimAreaChunkStatus(selectedChunk, chunk)));
        }
        return preview;
    }

    public List<ClaimPreviewChunk> getClaimRadiusPreview(ChunkPos center, int radius) {
        List<ClaimPreviewChunk> preview = new ArrayList<>();
        if (center == null) return preview;
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                preview.add(new ClaimPreviewChunk(chunk, getClaimChunkStatus(chunk, true)));
            }
        }
        return preview;
    }

    private boolean isPlayerTooFar(ChunkPos pos) {
        if (player == null || pos == null) return true;
        int diffX = Math.abs(player.chunkPosition().x - pos.x);
        int diffZ = Math.abs(player.chunkPosition().z - pos.z);
        return diffZ > MAX_PLAYER_CLAIM_DISTANCE_CHUNKS || diffX > MAX_PLAYER_CLAIM_DISTANCE_CHUNKS;
    }

    private boolean isPlayerInOverworld() {
        return minecraft.level != null
                && minecraft.level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }

    private int getMaxClaimChunks() {
        return ClientManager.configValueMaxClaimChunks > 0
                ? ClientManager.configValueMaxClaimChunks
                : RecruitsClaim.DEFAULT_MAX_SIZE;
    }

    private static boolean sameFaction(RecruitsFaction left, RecruitsFaction right) {
        if (left == null || right == null) return false;
        return Objects.equals(left.getStringID(), right.getStringID());
    }

    public enum ClaimPreviewStatus {
        VALID("gui.recruits.map.claim_preview.valid", 0x4433FF66),
        OCCUPIED("gui.recruits.map.claim_preview.occupied", 0x66FF4444),
        BUFFER_ZONE("gui.recruits.map.claim_preview.buffer", 0x66FF4444),
        TOO_FAR("gui.recruits.map.claim_preview.too_far", 0x66888888),
        NO_FACTION("gui.recruits.map.claim_preview.no_faction", 0x66888888),
        CLAIMING_DISABLED("gui.recruits.map.claim_preview.disabled", 0x66888888),
        NO_NEIGHBOR("gui.recruits.map.claim_preview.no_neighbor", 0x66555555),
        WRONG_FACTION("gui.recruits.map.claim_preview.wrong_faction", 0x66FF8844),
        CLAIM_FULL("gui.recruits.map.claim_preview.full", 0x66FFAA00),
        NO_PERMISSION("gui.recruits.map.claim_preview.no_permission", 0x66888888),
        NOT_ENOUGH_PAYMENT("gui.recruits.map.claim_preview.no_payment", 0x66888888),
        NO_CHUNK("gui.recruits.map.claim_preview.no_chunk", 0x66888888),
        WRONG_DIMENSION("gui.recruits.map.claim_preview.wrong_dimension", 0x66888888);

        private final String translationKey;
        private final int previewColor;

        ClaimPreviewStatus(String translationKey, int previewColor) {
            this.translationKey = translationKey;
            this.previewColor = previewColor;
        }

        public Component message() {
            return Component.translatable(translationKey);
        }

        public int previewColor() {
            return previewColor;
        }
    }

    public record ClaimPreviewChunk(ChunkPos chunk, ClaimPreviewStatus status) {}
}
