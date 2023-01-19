package com.talhanation.recruits.world;

import com.google.common.collect.Maps;
import com.talhanation.recruits.Main;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class RecruitsTeamSavedData extends SavedData {

    public static final String FILE_ID = "recruitsTeamSaveData";

    private static final Map<String, RecruitsTeam> teams = Maps.newHashMap();

    public RecruitsTeamSavedData(){
        this.setDirty();
    }

    public static RecruitsTeamSavedData get(ServerLevel level){
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(RecruitsTeamSavedData::load, RecruitsTeamSavedData::new, FILE_ID);
    }

    public static RecruitsTeamSavedData load(CompoundTag nbt) {
        RecruitsTeamSavedData data = new RecruitsTeamSavedData();
        if (nbt.contains("Teams", 9)) {
            data.loadTeams(nbt.getList("Teams", 10));
        }
        return data;
    }

    private static void loadTeams(ListTag list) {
        for(int i = 0; i < list.size(); ++i) {
            CompoundTag nbt = list.getCompound(i);
            String s = nbt.getString("TeamName");
            RecruitsTeam recruitsTeam = addPlayerTeam(s);

            if (nbt.contains("TeamName")) {
                recruitsTeam.setTeamName(nbt.getString("TeamName"));
            }
            if (nbt.contains("TeamLeaderID")) {
                recruitsTeam.setTeamLeaderID(nbt.getUUID("TeamLeaderID"));
            }
            if (nbt.contains("TeamLeaderName")) {
                recruitsTeam.setTeamLeaderName(nbt.getString("TeamLeaderName"));
            }
            if (nbt.contains("TeamBanner")) {
                recruitsTeam.setBanner((CompoundTag) nbt.get("TeamBanner"));

            }
            if (nbt.contains("Players")) {
                recruitsTeam.setPlayers(nbt.getInt("Players"));
            }
            if (nbt.contains("NPCs")) {
                recruitsTeam.setNPCs(nbt.getInt("NPCs"));
            }

            if (nbt.contains("JoinRequests")) {
                ListTag listtag = nbt.getList("JoinRequests", 10);
                for (int j = 0; j < listtag.size(); ++j) {
                    CompoundTag compoundtag = listtag.getCompound(j);
                        recruitsTeam.getJoinRequests().add(compoundtag.getString("Request"));
                }
            }
        }
    }

    private static RecruitsTeam addPlayerTeam(String s) {
        RecruitsTeam recruitsTeam = getTeamByName(s);
        if (recruitsTeam == null) {
            recruitsTeam = new RecruitsTeam();
            teams.put(s, recruitsTeam);

            return recruitsTeam;
        }
        else
            return recruitsTeam;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.put("Teams", this.saveTeams());;
        return nbt;
    }
    //ScoreboardSaveData
    private ListTag saveTeams() {
        ListTag listtag = new ListTag();

        for(RecruitsTeam recruitsTeam : teams.values()){

            CompoundTag nbt = new CompoundTag();
            nbt.putString("TeamName", recruitsTeam.getTeamName());
            nbt.putUUID("TeamLeaderID", recruitsTeam.getTeamLeaderUUID());
            nbt.putString("TeamLeaderName", recruitsTeam.getTeamLeaderName());
            nbt.put("TeamBanner", recruitsTeam.getBanner());
            nbt.putInt("Players", recruitsTeam.getPlayers());
            nbt.putInt("NPCs", recruitsTeam.getNPCs());

            ListTag listtag1 = new ListTag();
            for(String s : recruitsTeam.getJoinRequests()) {
                listtag1.add(StringTag.valueOf(s));
            }
            nbt.put("JoinRequests", listtag1);

            listtag.add(nbt);
        }
        return listtag;
    }
    @Nullable
    public static RecruitsTeam getTeamByName(String teamName) {
        Main.LOGGER.debug("get Teams" + teams.values());
        Main.LOGGER.debug("getTeamByName: Team: " + teams.get(teamName));
        return teams.get(teamName);
    }
    public Collection<RecruitsTeam> getTeams() {
        return teams.values();
    }

    public void addTeam(String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt) {
        RecruitsTeam recruitsTeam = new RecruitsTeam();
        recruitsTeam.setTeamName(teamName);
        recruitsTeam.setTeamLeaderID(leaderUUID);
        recruitsTeam.setTeamLeaderName(leaderName);
        recruitsTeam.setBanner(bannerNbt);

        teams.put(teamName, recruitsTeam);
    }
}
