package com.talhanation.recruits.world;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.CompanionOrders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Register of all owned companions (Commander, Captain, Messenger, Scout, Siege Engineer, Governor),
 * also of those whose chunk is not loaded. Workers are not in it.
 * <p>
 * Filled from the entity life cycle: join, leave, owner change and a position update every 100 ticks
 * (see {@code RecruitEvents} and {@code AbstractRecruitEntity}). Companions of existing worlds show up
 * once they were loaded, there is no world scan.
 * <p>
 * Base for the companion list of the Messenger and later for the chunk loading of the companions.
 */
public class RecruitsCompanionManager {
    /** A position change below this (squared, 8 blocks) is not worth a save. */
    private static final double MOVE_SAVE_DIST_SQR = 8 * 8;
    /** More orders than this do not wait for one companion; the oldest go first. */
    private static final int MAX_PENDING = 64;

    private RecruitsCompanionSaveData data;
    private Map<UUID, CompanionRecord> records = new HashMap<>();
    private final Map<UUID, Set<UUID>> byOwner = new HashMap<>();

    public void load(ServerLevel overworld) {
        data = RecruitsCompanionSaveData.get(overworld);
        records = data.records;
        byOwner.clear();
        for (CompanionRecord record : records.values()) {
            byOwner.computeIfAbsent(record.owner, k -> new HashSet<>()).add(record.uuid);
        }
    }

    public static boolean isCompanion(Entity entity) {
        return entity instanceof AbstractRecruitEntity && entity instanceof ICompanion;
    }

    /**
     * Adds or refreshes the record of the companion, or removes it if he has no owner (any more).
     * @param loaded false when he is just leaving the world with his chunk
     */
    public void update(AbstractRecruitEntity companion, boolean loaded) {
        if (!isCompanion(companion)) return;
        UUID owner = companion.isOwned() ? companion.getOwnerUUID() : null;
        if (owner == null) {
            remove(companion.getUUID());
            return;
        }

        CompanionRecord record = records.get(companion.getUUID());
        if (record == null) {
            record = new CompanionRecord(companion.getUUID());
            records.put(record.uuid, record);
        } else if (!owner.equals(record.owner)) {
            removeFromOwner(record);
        }

        record.owner = owner;
        byOwner.computeIfAbsent(owner, k -> new HashSet<>()).add(record.uuid);
        fill(record, companion);
        record.loaded = loaded;
        setDirty();
    }

    /** Called every 100 ticks per companion. Saves only if he moved more than 8 blocks or something else changed. */
    public void updatePosition(AbstractRecruitEntity companion) {
        CompanionRecord record = records.get(companion.getUUID());
        UUID owner = companion.isOwned() ? companion.getOwnerUUID() : null;
        if (record == null || owner == null || !owner.equals(record.owner)) {
            update(companion, true);
            return;
        }

        BlockPos pos = companion.blockPosition();
        String status = statusOf(companion);
        boolean changed = record.pos.distSqr(pos) > MOVE_SAVE_DIST_SQR
                || !record.dimension.equals(companion.level().dimension().location())
                || !record.status.equals(status)
                || record.atMission != ((ICompanion) companion).isAtMission()
                || !record.name.equals(companion.getName().getString());

        record.lastSeen = companion.level().getGameTime();
        record.loaded = true;
        if (changed) {
            fill(record, companion);
            setDirty();
        }

        // he is back in the world and ticking: what a Messenger left for him meanwhile is carried out now
        if (!record.pending.isEmpty()) {
            ListTag orders = record.pending;
            record.pending = new ListTag();
            CompanionOrders.applyAll(companion, orders, false);
            fill(record, companion);
            setDirty();
        }
    }

    /** Takes over how the companion looks right now (name, status, screen data), e.g. after orders reached him out of the world. */
    public void snapshot(AbstractRecruitEntity companion) {
        CompanionRecord record = records.get(companion.getUUID());
        if (record == null) return;
        record.name = companion.getName().getString();
        record.status = statusOf(companion);
        record.atMission = ((ICompanion) companion).isAtMission();
        record.synced = CompanionOrders.packSynced(companion);
        setDirty();
    }

    /** Orders for a companion who is not in the world. They are carried out when he is back, see {@link #updatePosition}. */
    public void addPending(UUID companion, ListTag orders) {
        CompanionRecord record = records.get(companion);
        if (record == null || orders.isEmpty()) return;
        record.pending.addAll(orders);
        while (record.pending.size() > MAX_PENDING) record.pending.remove(0);
        setDirty();
    }

    /**
     * A companion on a simulated journey is not in the world; the {@link SimulatedTravelManager} tells where he is.
     * Saves only if he moved more than 8 blocks or was in the world before.
     */
    public void updateTravelling(UUID companion, ResourceLocation dimension, BlockPos pos, long gameTime) {
        CompanionRecord record = records.get(companion);
        if (record == null) return;

        boolean changed = record.loaded || !record.dimension.equals(dimension) || record.pos.distSqr(pos) > MOVE_SAVE_DIST_SQR;
        record.loaded = false;
        record.lastSeen = gameTime;
        if (changed) {
            record.dimension = dimension;
            record.pos = pos;
            setDirty();
        }
    }

    public void remove(UUID companion) {
        CompanionRecord record = records.remove(companion);
        if (record == null) return;
        removeFromOwner(record);
        setDirty();
    }

    /** All companions of the owner, sorted by type and name. The records must not be changed. */
    public List<CompanionRecord> getCompanions(UUID owner) {
        Set<UUID> ids = byOwner.get(owner);
        if (ids == null || ids.isEmpty()) return List.of();

        List<CompanionRecord> list = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            CompanionRecord record = records.get(id);
            if (record != null) list.add(record);
        }
        list.sort(Comparator.comparing((CompanionRecord r) -> r.type.toString()).thenComparing(r -> r.name));
        return list;
    }

    @Nullable
    public CompanionRecord get(UUID companion) {
        return records.get(companion);
    }

    private void fill(CompanionRecord record, AbstractRecruitEntity companion) {
        record.type = EntityType.getKey(companion.getType());
        record.name = companion.getName().getString();
        record.dimension = companion.level().dimension().location();
        record.pos = companion.blockPosition();
        record.lastSeen = companion.level().getGameTime();
        record.status = statusOf(companion);
        record.atMission = ((ICompanion) companion).isAtMission();
        record.synced = CompanionOrders.packSynced(companion);
    }

    private void removeFromOwner(CompanionRecord record) {
        Set<UUID> ids = byOwner.get(record.owner);
        if (ids == null) return;
        ids.remove(record.uuid);
        if (ids.isEmpty()) byOwner.remove(record.owner);
    }

    private void setDirty() {
        if (data != null) data.setDirty();
    }

    /** Short status key for the companion list, empty if the companion has none. */
    public static String statusOf(AbstractRecruitEntity companion) {
        if (companion instanceof AbstractLeaderEntity leader && leader.getLeaderState() != null) {
            return leader.getLeaderState().name().toLowerCase(Locale.ROOT);
        }
        if (companion instanceof MessengerEntity messenger && messenger.getMessengerState() != null) {
            return messenger.getMessengerState().name().toLowerCase(Locale.ROOT);
        }
        if (companion instanceof GovernorEntity governor) {
            return governor.getGovernedClaim() != null ? "in_office" : "no_office";
        }
        return "";
    }
}
