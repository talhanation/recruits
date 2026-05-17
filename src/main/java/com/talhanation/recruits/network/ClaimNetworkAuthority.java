package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ClaimNetworkAuthority {
    private ClaimNetworkAuthority() {
    }

    @Nullable
    static RecruitsClaim readClaim(CompoundTag nbt) {
        try {
            return RecruitsClaim.fromNBT(nbt);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    static RecruitsFaction senderFaction(ServerPlayer player) {
        if (player == null || player.getTeam() == null) {
            return null;
        }
        return FactionEvents.recruitsFactionManager.getFactionByStringID(player.getTeam().getName());
    }

    @Nullable
    static RecruitsClaim claimByUuid(UUID claimId) {
        if (claimId == null) {
            return null;
        }
        return ClaimEvents.recruitsClaimManager.getAllClaims().stream()
                .filter(claim -> claimId.equals(claim.getUUID()))
                .findFirst()
                .orElse(null);
    }

    static boolean canManageClaim(ServerPlayer player, RecruitsClaim claim) {
        if (player == null || claim == null || claim.getOwnerFaction() == null) {
            return false;
        }
        RecruitsFaction senderFaction = senderFaction(player);
        if (senderFaction == null || !senderFaction.getStringID().equals(claim.getOwnerFactionStringID())) {
            return false;
        }
        if (player.getUUID().equals(senderFaction.getTeamLeaderUUID())) {
            return true;
        }
        return claim.getPlayerInfo() != null
                && player.getUUID().equals(claim.getPlayerInfo().getUUID())
                && FactionNetworkAuthority.memberByUuid(senderFaction, claim.getPlayerInfo()) != null;
    }

    static boolean isCreativeAdmin(ServerPlayer player) {
        return player != null && player.isCreative() && player.hasPermissions(2);
    }

    static boolean pay(ServerPlayer player, int cost) {
        if (cost <= 0 || isCreativeAdmin(player)) {
            return true;
        }
        if (!FactionEvents.playerHasEnoughEmeralds(player, cost)) {
            return false;
        }
        FactionEvents.doPayment(player, cost);
        return true;
    }

    static int newClaimCost(RecruitsFaction ownerFaction) {
        if (!RecruitsServerConfig.CascadeThePriceOfClaims.get()) {
            return RecruitsServerConfig.ClaimingCost.get();
        }
        int amount = 1;
        for (RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
            if (claim.getOwnerFaction() != null && claim.getOwnerFactionStringID().equals(ownerFaction.getStringID())) {
                amount++;
            }
        }
        return amount * RecruitsServerConfig.ClaimingCost.get();
    }

    static boolean isNearPlayer(ServerPlayer player, ChunkPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        ChunkPos playerChunk = player.chunkPosition();
        return Math.abs(playerChunk.x - pos.x) <= 4 && Math.abs(playerChunk.z - pos.z) <= 4;
    }

    static boolean isFiveByFiveArea(ChunkPos center, List<ChunkPos> chunks) {
        if (center == null || chunks == null || chunks.size() != 25) {
            return false;
        }
        Set<ChunkPos> chunkSet = new HashSet<>(chunks);
        if (chunkSet.size() != 25) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!chunkSet.contains(new ChunkPos(center.x + dx, center.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean hasForeignBufferConflict(RecruitsFaction ownerFaction, List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            if (hasForeignBufferConflict(ownerFaction, chunk)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasForeignBufferConflict(RecruitsFaction ownerFaction, ChunkPos chunk) {
        for (RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
            if (claim.getOwnerFaction() == null || claim.getOwnerFactionStringID().equals(ownerFaction.getStringID())) {
                continue;
            }
            for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);
                if (dx <= 3 && dz <= 3 && !(dx == 0 && dz == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean canRemoveChunk(RecruitsClaim claim, ChunkPos chunk) {
        if (claim == null || chunk == null || !claim.containsChunk(chunk)) {
            return false;
        }
        int unclaimedNeighbors = 0;
        for (ChunkPos neighbor : new ChunkPos[]{
                new ChunkPos(chunk.x + 1, chunk.z), new ChunkPos(chunk.x - 1, chunk.z),
                new ChunkPos(chunk.x, chunk.z + 1), new ChunkPos(chunk.x, chunk.z - 1)}) {
            if (!claim.containsChunk(neighbor)) {
                unclaimedNeighbors++;
            }
        }
        return unclaimedNeighbors >= 2;
    }

    static void recalculateCenter(RecruitsClaim claim) {
        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkPos pos : chunks) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minZ = Math.min(minZ, pos.z);
            maxZ = Math.max(maxZ, pos.z);
        }
        claim.setCenter(new ChunkPos((minX + maxX) / 2, (minZ + maxZ) / 2));
    }
}
