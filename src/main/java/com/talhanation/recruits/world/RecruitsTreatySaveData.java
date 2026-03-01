package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class RecruitsTreatySaveData extends SavedData {

    private static final String DATA_NAME = "treaties_data";
    private Map<String, Long> treaties = new HashMap<>();

    public RecruitsTreatySaveData() {
        this.setDirty();
    }

    public static RecruitsTreatySaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RecruitsTreatySaveData::load, RecruitsTreatySaveData::new, DATA_NAME);
    }

    public static RecruitsTreatySaveData load(CompoundTag nbt) {
        RecruitsTreatySaveData data = new RecruitsTreatySaveData();
        data.treaties = RecruitsTreatyManager.mapFromNbt(nbt.getCompound("treaties"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
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
