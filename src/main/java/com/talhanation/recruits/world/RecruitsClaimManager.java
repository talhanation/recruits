package com.talhanation.recruits.world;

import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.MessageToClientUpdateClaim;
import com.talhanation.recruits.network.MessageToClientUpdateClaims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecruitsClaimManager {
    private static final int CLAIMS_PER_SYNC_PACKET = 256;

    private final Map<ChunkPos, RecruitsClaim> claims = new HashMap<>();
    private final Map<UUID, RecruitsClaim> claimsById = new HashMap<>();
    private final Map<UUID, RecruitsClaim> activeSieges = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsClaimSaveData data = RecruitsClaimSaveData.get(level);
        this.claims.clear();
        this.claimsById.clear();
        this.activeSieges.clear();
        for (RecruitsClaim claim : data.getAllClaims()) {
            if (claim == null) continue;
            this.indexClaim(claim);
            if (claim.isUnderSiege) {
                this.activeSieges.put(claim.getUUID(), claim);
            }
        }
    }

    public void save(ServerLevel level) {
        RecruitsClaimSaveData data = RecruitsClaimSaveData.get(level);
        data.setAllClaims(new ArrayList<>(this.claimsById.values()));
        data.setDirty();
    }

    public void addOrUpdateClaim(ServerLevel level, RecruitsClaim claim) {
        if (claim == null) return;

        // ClaimEvent.Updated feuern – cancelable
        boolean isNew = !claimsById.containsKey(claim.getUUID());
        ClaimEvent.Updated updateEvent = new ClaimEvent.Updated(claim, level, isNew);
        if (MinecraftForge.EVENT_BUS.post(updateEvent)) return;

        this.removeClaimFromIndexes(claim.getUUID());

        if (!claim.isRemoved) {
            this.indexClaim(claim);
        }

        this.broadcastClaimUpdateToAll(level, claim);
    }

    public void removeClaim(RecruitsClaim claim) {
        if (claim == null) return;
        ServerLevel level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
        this.removeClaim(level, claim);
    }

    public boolean removeClaim(ServerLevel level, UUID claimId) {
        RecruitsClaim claim = this.getClaim(claimId);
        if (claim == null) return false;

        this.removeClaim(level, claim);
        claim.isRemoved = true;
        this.broadcastClaimUpdateToAll(level, claim);
        return true;
    }

    private void removeClaim(ServerLevel level, RecruitsClaim claim) {
        // ClaimEvent.Removed feuern
        MinecraftForge.EVENT_BUS.post(new ClaimEvent.Removed(claim, level));

        this.removeClaimFromIndexes(claim.getUUID());
        activeSieges.remove(claim.getUUID());
    }

    public void addActiveSiege(RecruitsClaim claim) {
        if (claim != null) {
            activeSieges.put(claim.getUUID(), claim);
        }
    }

    public void removeActiveSiege(RecruitsClaim claim) {
        if (claim != null) {
            activeSieges.remove(claim.getUUID());
        }
    }

    public Collection<RecruitsClaim> getActiveSieges() {
        return activeSieges.values();
    }

    public boolean isActiveSiege(RecruitsClaim claim) {
        return claim != null && activeSieges.containsKey(claim.getUUID());
    }

    // -------------------------------------------------------------------------

    @Nullable
    public RecruitsClaim getClaim(ChunkPos chunkPos) {
        return this.claims.get(chunkPos);
    }

    @Nullable
    public RecruitsClaim getClaim(int chunkX, int chunkZ) {
        return this.getClaim(new ChunkPos(chunkX, chunkZ));
    }

    @Nullable
    public RecruitsClaim getClaim(UUID claimId) {
        return claimId == null ? null : this.claimsById.get(claimId);
    }

    public List<RecruitsClaim> getAllClaims() {
        return new ArrayList<>(this.claimsById.values());
    }

    public boolean claimExists(RecruitsClaim claim, List<ChunkPos> allPos) {
        for (ChunkPos pos : allPos) {
            if (claims.containsKey(pos)) {
                return true;
            }
        }
        return false;
    }

    public static RecruitsClaim getClaimAt(ChunkPos pos, List<RecruitsClaim> allClaims) {
        for (RecruitsClaim claim : allClaims) {
            if (claim.containsChunk(pos)) {
                return claim;
            }
        }
        return null;
    }

    public void broadcastClaimsToAll(ServerLevel level) {
        List<RecruitsClaim> allClaims = this.getAllClaims();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            this.sendClaimsTo(player, allClaims);
        }
    }

    public void sendClaimsTo(ServerPlayer player) {
        if (player == null) return;
        this.sendClaimsTo(player, this.getAllClaims());
    }

    public void broadcastClaimUpdateTo(RecruitsClaim claim, List<ServerPlayer> players) {
        if (claim == null || players == null || players.isEmpty()) return;

        for (ServerPlayer player : players) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientUpdateClaim(claim));
        }
    }

    public void broadcastClaimUpdateToAll(ServerLevel level, RecruitsClaim claim) {
        if (level == null || claim == null) return;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientUpdateClaim(claim));
        }
    }

    private void sendClaimsTo(ServerPlayer player, List<RecruitsClaim> claims) {
        if (claims == null || claims.isEmpty()) {
            sendClaimBatch(player, List.of(), true, true);
            return;
        }

        for (int start = 0; start < claims.size(); start += CLAIMS_PER_SYNC_PACKET) {
            int end = Math.min(start + CLAIMS_PER_SYNC_PACKET, claims.size());
            boolean reset = start == 0;
            boolean complete = end >= claims.size();
            sendClaimBatch(player, claims.subList(start, end), reset, complete);
        }
    }

    private void sendClaimBatch(
            ServerPlayer player, List<RecruitsClaim> claims, boolean resetClaims, boolean syncComplete) {
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MessageToClientUpdateClaims(
                        claims,
                        RecruitsServerConfig.ClaimingCost.get(),
                        RecruitsServerConfig.ChunkCost.get(),
                        RecruitsServerConfig.MaxClaimChunks.get(),
                        RecruitsServerConfig.CascadeThePriceOfClaims.get(),
                        RecruitsServerConfig.AllowClaiming.get(),
                        RecruitsServerConfig.FogOfWarEnabled.get(),
                        FactionEvents.getCurrency(),
                        resetClaims,
                        syncComplete
                ));
    }

    private void indexClaim(RecruitsClaim claim) {
        if (claim == null || claim.getUUID() == null) return;

        this.claimsById.put(claim.getUUID(), claim);
        if (claim.getClaimedChunks() == null) return;

        for (ChunkPos pos : claim.getClaimedChunks()) {
            if (pos != null) {
                this.claims.put(pos, claim);
            }
        }
    }

    private void removeClaimFromIndexes(UUID claimId) {
        RecruitsClaim claim = this.claimsById.remove(claimId);
        if (claim == null || claim.getClaimedChunks() == null) return;

        for (ChunkPos pos : claim.getClaimedChunks()) {
            if (pos == null) continue;
            RecruitsClaim mappedClaim = this.claims.get(pos);
            if (mappedClaim != null && claimId.equals(mappedClaim.getUUID())) {
                this.claims.remove(pos);
            }
        }
    }
}
