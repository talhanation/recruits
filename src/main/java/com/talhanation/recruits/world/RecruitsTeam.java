package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class RecruitsTeam {
    public String stringID;
    public String teamDisplayName;
    public UUID teamLeaderID;
    public String teamLeaderName;
    public CompoundTag banner;
    public List<String> joinRequests = new ArrayList<>();
    public int players;
    public int npcs;
    public byte unitColor;
    public int teamColor;
    public int maxPlayers;
    public int maxNPCs;
    public int maxNPCsPerPlayer = -1;
    private int biome = -1;
    public RecruitsTeam(String stringID, String teamLeaderName, CompoundTag banner) {
        this.stringID = stringID;
        this.teamDisplayName = stringID;
        this.teamLeaderName = teamLeaderName;
        this.banner = banner;
    }

    public RecruitsTeam() {

    }

    public CompoundTag getBanner() {
        return banner;
    }

    public UUID getTeamLeaderUUID() {
        return teamLeaderID;
    }

    public String getStringID() {
        return stringID;
    }
    public String getTeamDisplayName() {
        return teamDisplayName;
    }

    public String getTeamLeaderName()  {
        return teamLeaderName;
    }

    public void setStringID(String stringID) {
        this.stringID = stringID;
    }

    public void setTeamDisplayName(String teamDisplayName) {
        this.teamDisplayName = teamDisplayName;
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

    public void setMaxPlayers(int max) {
        this.maxPlayers = max;
    }

    public void setMaxNPCs(int max) {
        this.maxNPCs = max;
    }

    public boolean addPlayerAsJoinRequest(String player) {
        if (!joinRequests.contains(player)){
            joinRequests.add(player);
            return true;
        }
        return false;
    }

    public void removeJoinRequest(String player) {
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

    public byte getUnitColor() {
        return unitColor;
    }

    public int  getMaxNPCsPerPlayer() {
        return maxNPCsPerPlayer;
    }
    public int getTeamColor() {
        return teamColor;
    }

    public int getMaxNPCs() {
        return maxNPCs;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void addNPCs(int x) {
        npcs += x;
        if (npcs < 0) npcs = 0;
    }

    public void addPlayer(int x) {
        players += x;
        if (players < 0) players = 0;
    }

    public void setUnitColor(byte unitColor) {
        this.unitColor = unitColor;
    }
    public void setTeamColor(int color) {
        this.teamColor = color;
    }

    public void setMaxNPCsPerPlayer(int maxNPCsPerPlayer) {
        this.maxNPCsPerPlayer = maxNPCsPerPlayer;
    }

    @Override
    public String toString() {
        return this.getStringID();
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("teamName", this.stringID);
        nbt.putString("teamDisplayName", this.teamDisplayName);
        nbt.putUUID("teamLeaderID", this.teamLeaderID);
        nbt.putString("teamLeaderName", this.teamLeaderName);
        nbt.put("banner", this.banner);

        ListTag joinRequestsTag = new ListTag();
        for (String request : joinRequests) {
            joinRequestsTag.add(StringTag.valueOf(request));
        }
        nbt.put("joinRequests", joinRequestsTag);

        nbt.putInt("players", this.players);
        nbt.putInt("npcs", this.npcs);
        nbt.putInt("maxPlayers", this.maxPlayers);
        nbt.putInt("maxNpcs", this.maxNPCs);
        nbt.putByte("unitColor", this.unitColor);
        nbt.putInt("teamColor", this.teamColor);
        nbt.putInt("maxPlayers", this.maxPlayers);
        nbt.putInt("biome", this.biome);
        nbt.putInt("maxNPCsPerPlayer", this.maxNPCsPerPlayer);

        return nbt;
    }

    public static RecruitsTeam fromNBT(CompoundTag nbt) {
        if(nbt == null || nbt.isEmpty()) {
            return null;
        }
        RecruitsTeam team = new RecruitsTeam();
        team.setStringID(nbt.getString("teamName"));
        if(nbt.getString("teamDisplayName").isEmpty()){
            team.setTeamDisplayName(team.getStringID());
        }
        else
            team.setTeamDisplayName(nbt.getString("teamDisplayName"));

        team.setTeamLeaderID(nbt.getUUID("teamLeaderID"));
        team.setTeamLeaderName(nbt.getString("teamLeaderName"));
        team.setBanner(nbt.getCompound("banner"));

        ListTag joinRequestsTag = nbt.getList("joinRequests", 8); // 8 is the ID for StringTag
        for (int i = 0; i < joinRequestsTag.size(); i++) {
            team.addPlayerAsJoinRequest(joinRequestsTag.getString(i));
        }

        team.setPlayers(nbt.getInt("players"));
        team.setNPCs(nbt.getInt("npcs"));
        team.setMaxPlayers(nbt.getInt("maxPlayers"));
        team.setMaxNPCs(nbt.getInt("maxNpcs"));
        team.setUnitColor(nbt.getByte("unitColor"));
        team.setTeamColor(nbt.getInt("teamColor"));
        team.maxPlayers = nbt.getInt("maxPlayers");
        team.biome = nbt.getInt("biome");
        team.setMaxNPCsPerPlayer(nbt.getInt("maxNPCsPerPlayer"));
        return team;
    }

    public static CompoundTag toNBT(List<RecruitsTeam> list) {
        CompoundTag nbt = new CompoundTag();
        ListTag teamList = new ListTag();

        for (RecruitsTeam team : list) {
            teamList.add(team.toNBT());
        }

        nbt.put("Teams", teamList);
        return nbt;
    }

    public static List<RecruitsTeam> getListFromNBT(CompoundTag nbt) {
        List<RecruitsTeam> list = new ArrayList<>();
        ListTag teamList = nbt.getList("Teams", 10); // 10 corresponds to CompoundTag type

        for (int i = 0; i < teamList.size(); i++) {
            CompoundTag teamTag = teamList.getCompound(i);
            list.add(RecruitsTeam.fromNBT(teamTag));
        }

        return list;
    }

    public enum PlayerRank {
        NONE,
        LEADER,
        CAPTAIN,
        COMMANDER,
    }
}

