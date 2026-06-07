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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WorldMapClaimController {
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
        return faction.getTeamLeaderUUID().equals(player.getUUID());
    }

    public boolean isPlayerClaimLeader(RecruitsClaim claim) {
        if (player == null || claim == null) return false;
        return claim.getPlayerInfo().getUUID().equals(player.getUUID());
    }

    public List<ChunkPos> getClaimArea(ChunkPos pos) {
        List<ChunkPos> area = new ArrayList<>();
        if (pos == null) return area;
        int range = 2;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                area.add(new ChunkPos(pos.x + dx, pos.z + dz));
            }
        }
        return area;
    }

    public void claimArea(ChunkPos selectedChunk) {
        if (!canPlayerPay(getClaimCost(ClientManager.ownFaction), player)) return;
        if (!ClientManager.configValueIsClaimingAllowed) return;
        if (!isPlayerInOverworld()) return;

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
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(newClaim));
    }

    public void claimChunk(ChunkPos selectedChunk) {
        if (!canPlayerPay(ClientManager.configValueChunkCost, player)) return;
        if (!ClientManager.configValueIsClaimingAllowed) return;
        if (!isPlayerInOverworld()) return;
        RecruitsClaim neighborClaim = getNeighborClaim(selectedChunk);
        if (neighborClaim == null) return;
        if (!Objects.equals(
                ClientManager.ownFaction.getStringID(), neighborClaim.getOwnerFaction().getStringID()))
            return;
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.equals(neighborClaim)) {
                neighborClaim.addChunk(selectedChunk);
                recalculateCenter(neighborClaim);
                break;
            }
        }
        Main.SIMPLE_CHANNEL.sendToServer(
                new MessageDoPayment(player.getUUID(), ClientManager.configValueChunkCost));
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(neighborClaim));
    }

    @Nullable
    public RecruitsClaim getNeighborClaim(ChunkPos chunk) {
        ChunkPos[] neighbors = {
            new ChunkPos(chunk.x + 1, chunk.z), new ChunkPos(chunk.x - 1, chunk.z),
            new ChunkPos(chunk.x, chunk.z + 1), new ChunkPos(chunk.x, chunk.z - 1)
        };
        for (ChunkPos neighbor : neighbors) {
            for (RecruitsClaim claim : ClientManager.recruitsClaims) {
                if (claim.containsChunk(neighbor)) return claim;
            }
        }
        return null;
    }

    public void recalculateCenter(RecruitsClaim claim) {
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

    public void centerOnClaim(RecruitsClaim claim) {
        if (claim == null || claim.getCenter() == null) return;
        camera.centerOnClaim(claim.getCenter());
    }

    public Rectangle getClaimScreenBounds(
            RecruitsClaim claim, double offsetX, double offsetZ, double scale) {
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
        if (pos == null || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(pos)) return false;
        List<ChunkPos> claimedChunks = claim.getClaimedChunks();
        if (!claimedChunks.contains(pos)) return false;
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
                if (claim.getOwnerFaction().getStringID().equals(ownerTeam.getStringID())) amount++;
            }
        }
        return amount * ClientManager.configValueClaimCost;
    }

    public boolean canPlayerPay(int cost, Player player) {
        return player.isCreative()
                || cost <= player.getInventory().countItem(ClientManager.currencyItemStack.getItem());
    }

    public static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction) {
        if (ownFaction == null) return false;
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim.getOwnerFaction() == null
                    || claim.getOwnerFaction().getStringID().equals(ownFaction.getStringID())) continue;
            for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);
                if (dx <= 3 && dz <= 3 && !(dx == 0 && dz == 0)) return true;
            }
        }
        return false;
    }

    public boolean canClaimChunk(ChunkPos pos) {
        if (!ClientManager.configValueIsClaimingAllowed
                || pos == null
                || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(pos)) return false;
        for (RecruitsClaim claim : ClientManager.recruitsClaims)
            if (claim.containsChunk(pos)) return false;
        RecruitsClaim neighbor = getNeighborClaim(pos);
        if (neighbor == null || neighbor.getClaimedChunks().size() >= RecruitsClaim.MAX_SIZE)
            return false;
        return !isInBufferZone(pos, ClientManager.ownFaction);
    }

    public boolean canClaimArea(ChunkPos selectedChunk, List<ChunkPos> areaChunks) {
        if (selectedChunk == null
                || areaChunks == null
                || areaChunks.isEmpty()
                || ClientManager.ownFaction == null) return false;
        if (isPlayerTooFar(selectedChunk)) return false;
        for (ChunkPos chunk : areaChunks) {
            for (RecruitsClaim claim : ClientManager.recruitsClaims)
                if (claim.containsChunk(chunk)) return false;
            if (isInBufferZone(chunk, ClientManager.ownFaction)) return false;
        }
        return true;
    }

    public List<ChunkPos> getClaimableChunks(ChunkPos center, int radius) {
        List<ChunkPos> result = new ArrayList<>();
        if (center == null || ClientManager.ownFaction == null) return result;
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (canClaimChunkRaw(chunk)) result.add(chunk);
            }
        }
        return result;
    }

    public boolean canClaimChunkRaw(ChunkPos pos) {
        for (RecruitsClaim claim : ClientManager.recruitsClaims)
            if (claim.containsChunk(pos)) return false;
        RecruitsClaim neighbor = getNeighborClaim(pos);
        if (neighbor == null) return false;
        return !isInBufferZone(pos, ClientManager.ownFaction);
    }

    private boolean isPlayerTooFar(ChunkPos pos) {
        if (pos == null) return true;
        int diffX = Math.abs(player.chunkPosition().x) - Math.abs(pos.x);
        int diffZ = Math.abs(player.chunkPosition().z) - Math.abs(pos.z);
        return Math.abs(diffZ) > 4 || Math.abs(diffX) > 4;
    }

    private boolean isPlayerInOverworld() {
        return minecraft.level != null
                && minecraft.level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }
}
