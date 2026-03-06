package com.talhanation.recruits.world;

import com.talhanation.recruits.ClaimEvent;
import net.minecraftforge.common.MinecraftForge;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.MessageToClientUpdateClaim;
import com.talhanation.recruits.network.MessageToClientUpdateClaims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
public class RecruitsClaimManager {
    private final Map<ChunkPos, RecruitsClaim> claims = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsClaimSaveData data = RecruitsClaimSaveData.get(level);
        this.claims.clear();
        for (RecruitsClaim claim : data.getAllClaims()) {
            for (ChunkPos pos : claim.getClaimedChunks()) {
                this.claims.put(pos, claim);
            }
        }
    }

    public void save(ServerLevel level) {
        RecruitsClaimSaveData data = RecruitsClaimSaveData.get(level);
        data.setAllClaims(new ArrayList<>(new HashSet<>(this.claims.values())));
        data.setDirty();
    }

    public void addOrUpdateClaim(ServerLevel level, RecruitsClaim claim) {
        if (claim == null) return;

        // ClaimEvent.Updated feuern – cancelable
        boolean isNew = claims.values().stream().noneMatch(c -> c.getUUID().equals(claim.getUUID()));
        ClaimEvent.Updated updateEvent = new ClaimEvent.Updated(claim, level, isNew);
        if (MinecraftForge.EVENT_BUS.post(updateEvent)) return;

        claims.entrySet().removeIf(entry -> entry.getValue().getUUID().equals(claim.getUUID()));

        if(!claim.isRemoved){
            for (ChunkPos pos : claim.getClaimedChunks()) {
                this.claims.put(pos, claim);
            }
        }

        this.broadcastClaimsToAll(level);
    }

    public void removeClaim(RecruitsClaim claim) {
        if (claim != null) {
            // ClaimEvent.Removed feuern
            ServerLevel level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
            MinecraftForge.EVENT_BUS.post(new ClaimEvent.Removed(claim, level));

            claims.entrySet().removeIf(entry -> entry.getValue().equals(claim));
        }
    }

    @Nullable
    public RecruitsClaim getClaim(ChunkPos chunkPos) {
        return this.claims.get(chunkPos);
    }

    @Nullable
    public RecruitsClaim getClaim(int chunkX, int chunkZ) {
        return this.getClaim(new ChunkPos(chunkX, chunkZ));
    }

    public List<RecruitsClaim> getAllClaims() {
        return new ArrayList<>(new HashSet<>(this.claims.values()));
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
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientUpdateClaims(
                            this.getAllClaims(),
                            RecruitsServerConfig.ClaimingCost.get(),
                            RecruitsServerConfig.ChunkCost.get(),
                            RecruitsServerConfig.CascadeThePriceOfClaims.get(),
                            RecruitsServerConfig.AllowClaiming.get(),
                            FactionEvents.getCurrency()
                    ));
        }
    }

    public void broadcastClaimUpdateTo(RecruitsClaim claim, List<ServerPlayer> players) {
        if (claim == null || players == null || players.isEmpty()) return;

        for (ServerPlayer player : players) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientUpdateClaim(claim));
        }
    }
}


