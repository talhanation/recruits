package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

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
    public byte color;

    public RecruitsTeam(String teamName, String teamLeaderName, CompoundTag banner) {
        this.teamName = teamName;
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

    public String getTeamName() {
        return teamName;
    }

    public String getTeamLeaderName() {
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
        if (!joinRequests.contains(player))
            joinRequests.add(player);
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

    public byte getColor() {
        return color;
    }

    public void addNPCs(int x) {
        npcs += x;
        if (npcs < 0) npcs = 0;
    }

    public void addPlayer(int x) {
        players += x;
        if (players < 0) players = 0;
    }

    public void setColor(byte color) {
        this.color = color;
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("teamName", this.teamName);
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
        nbt.putByte("color", this.color);

        return nbt;
    }

    // Method to load RecruitsTeam from NBT
    public static RecruitsTeam fromNBT(CompoundTag nbt) {
        RecruitsTeam team = new RecruitsTeam();
        team.setTeamName(nbt.getString("teamName"));
        team.setTeamLeaderID(nbt.getUUID("teamLeaderID"));
        team.setTeamLeaderName(nbt.getString("teamLeaderName"));
        team.setBanner(nbt.getCompound("banner"));

        ListTag joinRequestsTag = nbt.getList("joinRequests", 8); // 8 is the ID for StringTag
        for (int i = 0; i < joinRequestsTag.size(); i++) {
            team.addPlayerAsJoinRequest(joinRequestsTag.getString(i));
        }

        team.setPlayers(nbt.getInt("players"));
        team.setNPCs(nbt.getInt("npcs"));
        team.setColor(nbt.getByte("color"));

        return team;
    }

    // Method to convert a list of RecruitsTeam to NBT
    public static CompoundTag toNBT(List<RecruitsTeam> list) {
        CompoundTag nbt = new CompoundTag();
        ListTag teamList = new ListTag();

        for (RecruitsTeam team : list) {
            teamList.add(team.toNBT());
        }

        nbt.put("Teams", teamList);
        return nbt;
    }

    // Method to load a list of RecruitsTeam from NBT
    public static List<RecruitsTeam> getListFromNBT(CompoundTag nbt) {
        List<RecruitsTeam> list = new ArrayList<>();
        ListTag teamList = nbt.getList("Teams", 10); // 10 corresponds to CompoundTag type

        for (int i = 0; i < teamList.size(); i++) {
            CompoundTag teamTag = teamList.getCompound(i);
            list.add(RecruitsTeam.fromNBT(teamTag));
        }

        return list;
    }
}

