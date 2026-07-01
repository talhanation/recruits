package com.talhanation.recruits.world;
import net.minecraft.core.HolderLookup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecruitPlayerUnitSaveData extends SavedData {
    public static final Map<UUID, Integer> recruitCountMap = new HashMap<>();
    private static final String DATA_NAME = "recruit_player_unit_data";

    public RecruitPlayerUnitSaveData(){
        recruitCountMap.clear();
        this.setDirty();
    }

    public static RecruitPlayerUnitSaveData load(CompoundTag nbt, HolderLookup.Provider provider) {
        RecruitPlayerUnitSaveData data = new RecruitPlayerUnitSaveData();
        CompoundTag recruitCounts = nbt.getCompound("recruitCounts");

        for (String uuidKey : recruitCounts.getAllKeys()) {
            UUID uuid = UUID.fromString(uuidKey);
            int count = recruitCounts.getInt(uuidKey);
            recruitCountMap.put(uuid, count);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        CompoundTag recruitCounts = new CompoundTag();

        for (Map.Entry<UUID, Integer> entry : recruitCountMap.entrySet()) {
            recruitCounts.putInt(entry.getKey().toString(), entry.getValue());
        }

        nbt.put("recruitCounts", recruitCounts);
        return nbt;
    }

    public void setRecruitCount(UUID playerUUID, int count) {
        recruitCountMap.put(playerUUID, count);
        this.setDirty();
    }

    public Map<UUID, Integer> getRecruitCountMap(){
        return recruitCountMap;
    }
    public static RecruitPlayerUnitSaveData get(ServerLevel level) {
        return level
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(RecruitPlayerUnitSaveData::new, RecruitPlayerUnitSaveData::load), DATA_NAME);
    }
}
