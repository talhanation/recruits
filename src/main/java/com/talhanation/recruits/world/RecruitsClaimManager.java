package com.talhanation.recruits.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

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

    public void removeClaim(ChunkPos pos) {
        RecruitsClaim claim = this.claims.get(pos);
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

    public Collection<RecruitsClaim> getAllClaims() {
        return new HashSet<>(this.claims.values());
    }

    public boolean claimExists(RecruitsClaim claim) {
        for (ChunkPos pos : claim.getClaimedChunks()) {
            if (claims.containsKey(pos)) {
                return true;
            }
        }
        return false;
    }
}


