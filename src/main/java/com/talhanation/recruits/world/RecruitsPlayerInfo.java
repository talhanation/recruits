package com.talhanation.recruits.world;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecruitsPlayerInfo {
    private UUID uuid;
    private String name;
    private String teamName;
    @Nullable
    private PlayerTeam playerTeam;

    public RecruitsPlayerInfo(UUID uuid, String name) {
        this(uuid, name, "No Team");
    }
    public RecruitsPlayerInfo(UUID uuid, String name, String teamName) {
        this.uuid = uuid;
        this.name = name;
        this.teamName = teamName;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public PlayerTeam getPlayerTeam() {
        return playerTeam;
    }

    public void setPlayerTeam(@NotNull PlayerTeam playerTeam) {
        this.playerTeam = playerTeam;
        this.teamName = playerTeam.getName();
    }

    @Override
    public String toString() {
        return "{" +
                ", uuid=" + uuid +
                ", name=" + name +
                ", playerTeam=" + playerTeam +
                '}';
    }


    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("UUID", uuid);
        nbt.putString("Name", name);
        nbt.putString("TeamName", teamName);
        return nbt;
    }

    public static RecruitsPlayerInfo getFromNBT(CompoundTag nbt) {
        UUID uuid = nbt.getUUID("UUID");
        String name = nbt.getString("Name");
        String teamName = nbt.getString("TeamName");

        return new RecruitsPlayerInfo(uuid, name, teamName);
    }

    public static CompoundTag toNBT(List<RecruitsPlayerInfo> list) {
        CompoundTag nbt = new CompoundTag();
        ListTag playerList = new ListTag();

        for (RecruitsPlayerInfo playerInfo : list) {
            CompoundTag playerTag = playerInfo.toNBT();
            playerList.add(playerTag);
        }

        nbt.put("Players", playerList);
        return nbt;
    }

    public static List<RecruitsPlayerInfo> getListFromNBT(CompoundTag nbt) {
        List<RecruitsPlayerInfo> list = new ArrayList<>();
        ListTag playerList = nbt.getList("Players", 10);

        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            RecruitsPlayerInfo playerInfo = RecruitsPlayerInfo.getFromNBT(playerTag);
            list.add(playerInfo);
        }

        return list;
    }


}