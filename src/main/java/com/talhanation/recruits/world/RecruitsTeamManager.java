package com.talhanation.recruits.world;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import javax.annotation.Nullable;
import java.util.*;

public class RecruitsTeamManager {
    private final Map<String, RecruitsTeam> teams = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitsTeamSaveData data = RecruitsTeamSaveData.get(level);
        teams.clear();
        teams.putAll(data.getTeams());

        teams.values().forEach(this::loadConfig);
    }

    public void loadConfig(RecruitsTeam team) {
        team.maxPlayers = RecruitsServerConfig.MaxPlayersInFaction.get();
        team.maxNPCs = RecruitsServerConfig.MaxNPCsInFaction.get();
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
    public RecruitsTeam getTeamByStringID(String stringID) {
        return teams.get(stringID);
    }

    public List<ServerPlayer> getPlayersInTeam(String stringID, ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam playerTeam = scoreboard.getPlayerTeam(stringID);

        List<ServerPlayer> list = new ArrayList<>();

        if(playerTeam != null){
            for(ServerPlayer p : level.players()){
                if(playerTeam.getPlayers().contains(p.getName().getString())){
                    list.add(p);
                }
            }
        }

        return list;
    }
    @Nullable
    public ServerPlayer getTeamLeader(RecruitsTeam recruitsTeam, ServerLevel level) {
        for(ServerPlayer p : level.players()){
            if(p.getUUID().equals(recruitsTeam.getTeamLeaderUUID())){
                return p;
            }
        }
        return null;
    }

    public void addTeam(String teamName, UUID leaderUUID, String leaderName, CompoundTag bannerNbt, byte color, ChatFormatting teamColor) {
        RecruitsTeam recruitsTeam = new RecruitsTeam();
        recruitsTeam.setStringID(teamName);
        recruitsTeam.setTeamDisplayName(teamName);
        recruitsTeam.setTeamLeaderID(leaderUUID);
        recruitsTeam.setTeamLeaderName(leaderName);
        recruitsTeam.setBanner(bannerNbt);
        recruitsTeam.setUnitColor(color);
        recruitsTeam.setTeamColor(teamColor.getId());
        recruitsTeam.setMaxNPCsPerPlayer(RecruitsServerConfig.MaxRecruitsForPlayer.get());
        recruitsTeam.setMaxPlayers(RecruitsServerConfig.MaxPlayersInFaction.get());
        recruitsTeam.setMaxNPCs(RecruitsServerConfig.MaxNPCsInFaction.get());
        teams.put(teamName, recruitsTeam);
    }
    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }

    public boolean isNameInUse(String teamName) {

        List<RecruitsTeam> list = getTeams().stream().toList();
        boolean equ = false;
        for(RecruitsTeam recruitsTeam : list){
            equ = recruitsTeam.getStringID().toLowerCase().strip().equals(teamName.toLowerCase());
        }
        return equ;
    }

    public boolean isBannerInUse(CompoundTag bannerNbt){
        if(bannerNbt != null){
            for(RecruitsTeam recruitsTeam : getTeams()){
                return bannerNbt.equals(recruitsTeam.getBanner());
            }
        }
        return false;
    }

    public static boolean isBannerBlank(ItemStack itemStack){
        CompoundTag compoundtag = BlockItem.getBlockEntityData(itemStack);
        return compoundtag == null || !compoundtag.contains("Patterns");
    }

    public boolean canPlayerJoin(RecruitsTeam recruitsTeam){
        int config = RecruitsServerConfig.MaxPlayersInFaction.get();
        if(config == 0) {
            return true;
        }
        else
            return config <= recruitsTeam.getPlayers();
    }

    public boolean canRecruitJoin(RecruitsTeam recruitsTeam){
        int config = RecruitsServerConfig.MaxPlayersInFaction.get();
        if(config == 0){
            return true;
        }
        else
            return RecruitsServerConfig.MaxNPCsInFaction.get() < recruitsTeam.getNPCs();
    }
}

