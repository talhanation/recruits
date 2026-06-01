package com.talhanation.recruits.compat.corpse;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RecruitCorpseAppearance {

    private static final long MAGIC = 0x5250434F52505345L;
    private static final long VARIANT_MASK = 0x1FL;
    private static final long BIOME_MASK = 0x7L;
    private static final long COLOR_MASK = 0x1FL;
    private static final int BIOME_SHIFT = 5;
    private static final int COLOR_SHIFT = 8;
    private static final int HAS_TEAM_SHIFT = 13;
    private static final int COMPANION_SHIFT = 14;
    private static final int POSE_SHIFT = 15;
    private static final int ENTITY_HASH_SHIFT = 17;
    private static final long ENTITY_HASH_MASK = 0xFFFFFFFFL;

    public static final byte FULL_PLAYER_MODEL = 0x7F;

    private RecruitCorpseAppearance() {
    }

    public static UUID encode(AbstractRecruitEntity recruit, byte poseType) {
        // Corpse only syncs a player UUID to the client, so recruit render data is packed into a namespaced UUID.
        long data = 0L;
        data |= ((long) recruit.getVariant()) & VARIANT_MASK;
        data |= ((((long) recruit.getBiome()) & BIOME_MASK) << BIOME_SHIFT);
        data |= ((((long) recruit.getColor()) & COLOR_MASK) << COLOR_SHIFT);

        if (recruit.getTeam() != null) {
            data |= 1L << HAS_TEAM_SHIFT;
        }
        if (recruit instanceof ICompanion) {
            data |= 1L << COMPANION_SHIFT;
        }
        data |= ((((long) poseType) & 0x3L) << POSE_SHIFT);
        data |= (Integer.toUnsignedLong(RecruitCorpseEntityTypeResolver.getEntityTypeHash(recruit)) & ENTITY_HASH_MASK) << ENTITY_HASH_SHIFT;

        return new UUID(MAGIC, data);
    }

    public static boolean isRecruitCorpse(@Nullable UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == MAGIC;
    }

    @Nullable
    public static Data decode(@Nullable UUID uuid) {
        if (!isRecruitCorpse(uuid)) {
            return null;
        }

        long data = uuid.getLeastSignificantBits();
        int variant = (int) (data & VARIANT_MASK);
        byte biome = (byte) ((data >> BIOME_SHIFT) & BIOME_MASK);
        byte color = (byte) ((data >> COLOR_SHIFT) & COLOR_MASK);
        boolean hasTeam = ((data >> HAS_TEAM_SHIFT) & 1L) != 0L;
        boolean companion = ((data >> COMPANION_SHIFT) & 1L) != 0L;
        byte poseType = (byte) ((data >> POSE_SHIFT) & 0x3L);
        int entityTypeHash = (int) ((data >> ENTITY_HASH_SHIFT) & ENTITY_HASH_MASK);

        return new Data(variant, biome, color, hasTeam, companion, poseType, entityTypeHash);
    }

    public record Data(int variant, byte biome, byte color, boolean hasTeam, boolean companion, byte poseType, int entityTypeHash) {
    }
}
