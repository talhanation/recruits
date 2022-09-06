package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RecruitsTeamSavedData extends SavedData {
    public static String teamName;
    public static UUID teamLeader;
    public static CompoundTag banner;

    public RecruitsTeamSavedData(){
        super();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putString("TeamName", teamName);
        nbt.putUUID("TeamLeader", teamLeader);
        nbt.put("TeamBanner", banner);
        return nbt;
    }
    public static RecruitsTeamSavedData load(CompoundTag nbt) {
        RecruitsTeamSavedData data = new RecruitsTeamSavedData();
        if (nbt.contains("TeamName")) {
            teamName = nbt.getString("TeamName");
        }
        if (nbt.contains("TeamLeader")) {
            teamLeader = nbt.getUUID("TeamLeader");
        }
        if (nbt.contains("TeamBanner")) {
            banner = (CompoundTag) nbt.get("TeamBanner");
        }
        return data;
    }

    public CompoundTag getBanner() {
        return banner;
    }

    public UUID getTeamLeader(){
        return teamLeader;
    }

    public String getTeam(){
        return teamName;
    }

    public static void setTeam(String teamname) {
        teamName = teamname;
    }

    public static void setTeamLeader(UUID uuid) {
        teamLeader = uuid;
    }

    public static void setBanner(CompoundTag nbt) {
        banner = nbt;
    }
}
