package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class RecruitsTeamManager {
    private final Map<String, RecruitsTeam> teams = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsTeamSaveData data = RecruitsTeamSaveData.get(level);
        teams.clear();
        teams.putAll(data.getTeams());
    }

    public void save(ServerLevel level) {
        RecruitsTeamSaveData data = RecruitsTeamSaveData.get(level);
        data.setTeams(teams);
        data.setDirty();
    }

    public Collection<RecruitsTeam> getTeams() {
        return teams.values();
    }

    @Nullable
    public RecruitsTeam getTeamByName(String teamName) {
        return teams.get(teamName);
    }

    public void addTeam(String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt, byte color) {
        RecruitsTeam recruitsTeam = new RecruitsTeam();
        recruitsTeam.setTeamName(teamName);
        recruitsTeam.setTeamLeaderID(leaderUUID);
        recruitsTeam.setTeamLeaderName(leaderName);
        recruitsTeam.setBanner(bannerNbt);
        recruitsTeam.setColor(color);

        teams.put(teamName, recruitsTeam);
    }

    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }

    public boolean isNameInUse(String teamName) {

        List<RecruitsTeam> list = getTeams().stream().toList();
        boolean equ = false;
        for(RecruitsTeam recruitsTeam : list){
            equ = recruitsTeam.getTeamName().toLowerCase().strip().equals(teamName.toLowerCase());
        }
        return equ;
    }

    public boolean isBannerInUse(ServerLevel level, CompoundTag bannerNbt){
        if(bannerNbt != null){
            for(RecruitsTeam recruitsTeam : getTeams()){
                return bannerNbt.equals(recruitsTeam.getBanner());
            }
        }
        return false;
    }

    public boolean isBannerBlank(ItemStack itemStack){
        CompoundTag compoundtag = BlockItem.getBlockEntityData(itemStack);
        return compoundtag == null || !compoundtag.contains("Patterns");
    }
}

