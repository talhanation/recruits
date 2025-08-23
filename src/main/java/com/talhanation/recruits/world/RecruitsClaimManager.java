package com.talhanation.recruits.world;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.MessageToClientUpdateClaim;
import com.talhanation.recruits.network.MessageToClientUpdateClaims;
import com.talhanation.recruits.network.MessageToClientUpdateCommandScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
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

    public void addOrUpdateClaim(RecruitsClaim claim) {
        for (ChunkPos pos : claim.getClaimedChunks()) {
            this.claims.put(pos, claim);
        }
    }

    public void removeClaim(RecruitsClaim claim) {
        if (claim != null) {
            for (ChunkPos cp : claim.getClaimedChunks()) {
                this.claims.remove(cp);
            }
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
        return new HashSet<>(this.claims.values()).stream().toList();
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
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player),
                    new MessageToClientUpdateClaims(this.getAllClaims(), RecruitsServerConfig.ClaimingCost.get(), RecruitsServerConfig.ChunkCost.get(), RecruitsServerConfig.CascadeThePriceOfClaims.get(), TeamEvents.getCurrency()));
        }
    }

    public void broadcastClaimUpdateTo(RecruitsClaim claim, List<ServerPlayer> players) {
        if (claim == null || players == null || players.isEmpty()) return;

        for (ServerPlayer player : players) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player),
                    new MessageToClientUpdateClaim(claim));
        }
    }
}


