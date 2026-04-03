package com.talhanation.recruits.world;

import com.talhanation.recruits.world.RecruitsDiplomacyManager.DiplomacyStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class RecruitsDiplomacySaveData extends SavedData {

    public static final Map<String, Map<String, DiplomacyStatus>> diplomacyMap = new HashMap<>();
    public static final Map<UUID, String> embargoMap = new HashMap<>();

    private static final String DATA_NAME = "diplomacy_data";

    public RecruitsDiplomacySaveData() {
        diplomacyMap.clear();
        embargoMap.clear();
        this.setDirty();
    }

    public static RecruitsDiplomacySaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RecruitsDiplomacySaveData::load, RecruitsDiplomacySaveData::new, DATA_NAME);
    }

    public static RecruitsDiplomacySaveData load(CompoundTag nbt) {
        RecruitsDiplomacySaveData data = new RecruitsDiplomacySaveData();

        CompoundTag teamsTag = nbt.getCompound("teams");
        for (String teamKey : teamsTag.getAllKeys()) {
            CompoundTag relationsTag = teamsTag.getCompound(teamKey);
            Map<String, DiplomacyStatus> relations = new HashMap<>();
            for (String relationKey : relationsTag.getAllKeys()) {
                byte relationStatusByte = relationsTag.getByte(relationKey);
                relations.put(relationKey, DiplomacyStatus.fromByte(relationStatusByte));
            }
            diplomacyMap.put(teamKey, relations);
        }

        CompoundTag embargoesTag = nbt.getCompound("embargoes");
        for (String uuidKey : embargoesTag.getAllKeys()) {
            String csv = embargoesTag.getString(uuidKey);
            if (!csv.isEmpty()) {
                embargoMap.put(UUID.fromString(uuidKey), csv);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag teamsTag = new CompoundTag();
        for (Map.Entry<String, Map<String, DiplomacyStatus>> teamEntry : diplomacyMap.entrySet()) {
            CompoundTag relationsTag = new CompoundTag();
            for (Map.Entry<String, DiplomacyStatus> relationEntry : teamEntry.getValue().entrySet()) {
                relationsTag.putByte(relationEntry.getKey(), relationEntry.getValue().getByteValue());
            }
            teamsTag.put(teamEntry.getKey(), relationsTag);
        }
        nbt.put("teams", teamsTag);

        CompoundTag embargoesTag = new CompoundTag();
        embargoMap.forEach((uuid, teamIDs) -> {
            if (!teamIDs.isEmpty()) {
                embargoesTag.putString(uuid.toString(), teamIDs);
            }
        });
        nbt.put("embargoes", embargoesTag);

        return nbt;
    }

    public void setRelation(String team, String otherTeam, byte relationByte) {
        DiplomacyStatus relation = DiplomacyStatus.fromByte(relationByte);
        diplomacyMap.computeIfAbsent(team, k -> new HashMap<>()).put(otherTeam, relation);
        this.setDirty();
    }

    public Map<String, Map<String, DiplomacyStatus>> getDiplomacyMap() {
        return diplomacyMap;
    }

    public void setEmbargoMap(Map<UUID, String> map) {
        embargoMap.clear();
        embargoMap.putAll(map);
        this.setDirty();
    }

    public Map<UUID, String> getEmbargoMap() {
        return embargoMap;
    }
}
