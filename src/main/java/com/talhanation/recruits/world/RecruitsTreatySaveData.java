package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class RecruitsTreatySaveData extends SavedData {

    private static final String DATA_NAME = "treaties_data";
    private static final SavedData.Factory<RecruitsTreatySaveData> FACTORY = new SavedData.Factory<>(RecruitsTreatySaveData::new, RecruitsTreatySaveData::load);
    private Map<String, Long> treaties = new HashMap<>();

    public RecruitsTreatySaveData() {
        this.setDirty();
    }

    public static RecruitsTreatySaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static RecruitsTreatySaveData load(CompoundTag nbt, HolderLookup.Provider registries) {
        RecruitsTreatySaveData data = new RecruitsTreatySaveData();
        data.treaties = RecruitsTreatyManager.mapFromNbt(nbt.getCompound("treaties"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.put("treaties", RecruitsTreatyManager.mapToNbt(treaties));
        return nbt;
    }

    public Map<String, Long> getTreaties() {
        return treaties;
    }

    public void setTreaties(Map<String, Long> treaties) {
        this.treaties = new HashMap<>(treaties);
        this.setDirty();
    }
}
