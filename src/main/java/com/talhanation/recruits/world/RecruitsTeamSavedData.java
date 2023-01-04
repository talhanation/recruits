package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class RecruitsTeamSavedData extends SavedData {
    public static String teamName;
    public static UUID teamLeaderID;
    public static String teamLeaderName;
    public static CompoundTag banner;
    public static List<String> joinRequests;

    public RecruitsTeamSavedData(){
        super();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putString("TeamName", teamName);
        nbt.putUUID("TeamLeaderID", teamLeaderID);
        nbt.putString("TeamLeaderName", teamLeaderName);
        nbt.put("TeamBanner", banner);

        // compaund list save
        return nbt;
    }
    public static RecruitsTeamSavedData load(CompoundTag nbt) {
        RecruitsTeamSavedData data = new RecruitsTeamSavedData();
        if (nbt.contains("TeamName")) {
            teamName = nbt.getString("TeamName");
        }
        if (nbt.contains("TeamLeaderID")) {
            teamLeaderID = nbt.getUUID("TeamLeaderID");
        }
        if (nbt.contains("TeamLeaderName")) {
            teamLeaderName = nbt.getString("TeamLeaderName");
        }
        if (nbt.contains("TeamBanner")) {
            banner = (CompoundTag) nbt.get("TeamBanner");
        }
        //BeehiveBlock //for ListTag example
        return data;
    }

    public CompoundTag getBanner() {
        return banner;
    }

    public UUID getTeamLeaderID(){
        return teamLeaderID;
    }

    public String getTeam(){
        return teamName;
    }
    public String getTeamLeaderName(){
        return teamLeaderName;
    }

    public static void setTeam(String teamname) {
        teamName = teamname;
    }

    public static void setTeamLeaderID(UUID uuid) {
        teamLeaderID = uuid;
    }
    public static void setTeamLeaderName(String leaderName) {
        teamLeaderName = leaderName;
    }

    public static void setBanner(CompoundTag nbt) {
        banner = nbt;
    }

    public static void addPlayerAsJoinRequest(String player) {
        joinRequests.add(player);
    }

    public List<String> getJoinRequests() {
        return joinRequests;
    }
}
