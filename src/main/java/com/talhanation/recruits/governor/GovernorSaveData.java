package com.talhanation.recruits.governor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Governor data of all claims, stored separately from the claims so the claim sync packets stay small.
 * Claims only exist in the overworld, so this lives in the overworld data storage.
 */
public class GovernorSaveData extends SavedData {
    private static final String DATA_NAME = "recruits_governors";

    private final List<ClaimGovernorData> entries = new ArrayList<>();
    /** While the server runs, the live data of the manager is written (not a copy from the last save event). */
    @Nullable
    private GovernorManager liveSource;

    public GovernorSaveData() {
    }

    public void setLiveSource(@Nullable GovernorManager manager) {
        this.liveSource = manager;
    }

    /** Small data, always written when the level saves, so no change gets lost between autosaves. */
    @Override
    public boolean isDirty() {
        return liveSource != null || super.isDirty();
    }

    public static GovernorSaveData load(CompoundTag nbt) {
        GovernorSaveData data = new GovernorSaveData();
        ListTag list = nbt.getList("governors", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.hasUUID("claim")) continue;
            data.entries.add(ClaimGovernorData.fromNBT(tag));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        Collection<ClaimGovernorData> source = liveSource != null ? liveSource.getAll() : entries;
        for (ClaimGovernorData entry : source) list.add(entry.toNBT());
        nbt.put("governors", list);
        return nbt;
    }

    public List<ClaimGovernorData> getEntries() {
        return entries;
    }

    public void setEntries(Collection<ClaimGovernorData> data) {
        entries.clear();
        entries.addAll(data);
        setDirty();
    }

    public static GovernorSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(GovernorSaveData::load, GovernorSaveData::new, DATA_NAME);
    }
}
