package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecruitsTeam {
    public String teamName;
    public UUID teamLeaderID;
    public String teamLeaderName;
    public CompoundTag banner;
    public List<String> joinRequests = new ArrayList<>();
    public int players;
    public int npcs;

    public RecruitsTeam() {
    }

    public CompoundTag getBanner() {
        return banner;
    }

    public UUID getTeamLeaderUUID(){
        return teamLeaderID;
    }

    public String getTeamName(){
        return teamName;
    }
    public String getTeamLeaderName(){
        return teamLeaderName;
    }

    public void setTeamName(String teamname) {
        teamName = teamname;
    }

    public void setTeamLeaderID(UUID uuid) {
        teamLeaderID = uuid;
    }
    public void setTeamLeaderName(String leaderName) {
        teamLeaderName = leaderName;
    }

    public void setBanner(CompoundTag nbt) {
        banner = nbt;
    }

    public void setPlayers(int players) {
        this.players = players;
    }
    public void setNPCs(int npcs) {
        this.npcs = npcs;
    }

    public void addPlayerAsJoinRequest(String player) {
        if(!joinRequests.contains(player))
            joinRequests.add(player);
    }

    public void removeJoinRequest(String player){
        joinRequests.remove(player);
    }

    public List<String> getJoinRequests() {
        return joinRequests;
    }

    public int getNPCs() {
        return npcs;
    }

    public int getPlayers() {
        return players;
    }

    public void addNPCs(int x) {
        npcs += x;
        if(npcs < 0) npcs = 0;
    }

    public void addPlayer(int x){
        players += x;
        if(players < 0) players = 0;
    }
}
