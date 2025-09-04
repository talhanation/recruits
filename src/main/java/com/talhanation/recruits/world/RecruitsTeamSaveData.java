package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

public class RecruitsTeamSaveData extends SavedData {

    public static final String FILE_ID = "recruitsTeamSaveData";
    private Map<String, RecruitsFaction> teams = new HashMap<>();

    public static RecruitsTeamSaveData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(RecruitsTeamSaveData::load, RecruitsTeamSaveData::new, FILE_ID);
    }

    public static RecruitsTeamSaveData load(CompoundTag nbt) {
        RecruitsTeamSaveData data = new RecruitsTeamSaveData();
        if (nbt.contains("Teams", 9)) {
            data.teams = loadTeams(nbt.getList("Teams", 10));
        }
        return data;
    }

    private static Map<String, RecruitsFaction> loadTeams(ListTag list) {
        Map<String, RecruitsFaction> loadedTeams = new HashMap<>();
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag nbt = list.getCompound(i);
            RecruitsFaction recruitsFaction = new RecruitsFaction();

            recruitsFaction.setStringID(nbt.getString("TeamName"));
            recruitsFaction.setTeamDisplayName(nbt.getString("TeamDisplayName"));
            recruitsFaction.setTeamLeaderID(nbt.getUUID("TeamLeaderID"));
            recruitsFaction.setTeamLeaderName(nbt.getString("TeamLeaderName"));
            recruitsFaction.setBanner((CompoundTag) nbt.get("TeamBanner"));
            recruitsFaction.setPlayers(nbt.getInt("Players"));
            recruitsFaction.setNPCs(nbt.getInt("NPCs"));

            recruitsFaction.setMaxPlayers(nbt.getInt("MaxPlayers"));
            recruitsFaction.setMaxNPCs(nbt.getInt("MaxNPCs"));

            ListTag joinRequestsList = nbt.getList("JoinRequests", 8);
            for (int j = 0; j < joinRequestsList.size(); ++j) {
                recruitsFaction.getJoinRequests().add(joinRequestsList.getString(j));
            }

            recruitsFaction.setUnitColor(nbt.getByte("Color"));
            recruitsFaction.setTeamColor(nbt.getInt("TeamColor"));
            recruitsFaction.setMaxNPCsPerPlayer(nbt.getInt("maxNpcsPerPlayer"));

            loadedTeams.put(recruitsFaction.getStringID(), recruitsFaction);
        }
        return loadedTeams;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.put("Teams", saveTeams());
        return nbt;
    }

    private ListTag saveTeams() {
        ListTag listTag = new ListTag();
        for (RecruitsFaction team : teams.values()) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("TeamName", team.getStringID());
            nbt.putString("TeamDisplayName", team.getTeamDisplayName());
            nbt.putUUID("TeamLeaderID", team.getTeamLeaderUUID());
            nbt.putString("TeamLeaderName", team.getTeamLeaderName());
            nbt.put("TeamBanner", team.getBanner());
            nbt.putInt("Players", team.getPlayers());
            nbt.putInt("NPCs", team.getNPCs());

            nbt.putInt("MaxPlayers", team.getMaxPlayers());
            nbt.putInt("MaxNPCs", team.getMaxNPCs());

            ListTag joinRequestsTag = new ListTag();
            for (String request : team.getJoinRequests()) {
                joinRequestsTag.add(StringTag.valueOf(request));
            }
            nbt.put("JoinRequests", joinRequestsTag);
            nbt.putByte("Color", team.getUnitColor());
            nbt.putInt("TeamColor", team.getTeamColor());
            nbt.putInt("maxNpcsPerPlayer", team.getMaxNPCsPerPlayer());

            listTag.add(nbt);
        }
        return listTag;
    }

    public Map<String, RecruitsFaction> getTeams() {
        return teams;
    }

    public void setTeams(Map<String, RecruitsFaction> teams) {
        this.teams = teams;
    }
}

