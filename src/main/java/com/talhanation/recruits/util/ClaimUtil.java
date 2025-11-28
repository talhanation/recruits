package com.talhanation.recruits.util;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ClaimUtil {
    public static List<LivingEntity> getLivingEntitiesInClaim(Level level, RecruitsClaim claim, Predicate<LivingEntity> filter) {
        List<LivingEntity> list = new ArrayList<>();
        for (ChunkPos chunkPos : claim.getClaimedChunks()) {
            list.addAll(getLivingEntitiesInChunk(level, chunkPos, filter));
        }
        return list;
    }

    public static List<LivingEntity> getLivingEntitiesInChunk(Level level, ChunkPos chunkPos, Predicate<LivingEntity> filter) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        int minY = -64;
        int maxY = level.getMaxBuildHeight();
        AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY, maxZ + 1);
        return level.getEntitiesOfClass(LivingEntity.class, box, filter);
    }
    public static List<LivingEntity> getLivingEntitiesInArea(Level level, AABB area, Predicate<LivingEntity> filter) {
        return level.getEntitiesOfClass(LivingEntity.class, area, filter);
    }
}

