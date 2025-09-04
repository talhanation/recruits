package com.talhanation.recruits.world;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecruitsPlayerInfo {
    private UUID uuid;
    private String name;
    @Nullable
    private final RecruitsFaction recruitsFaction;

    public RecruitsPlayerInfo(UUID uuid, String name) {
        this(uuid, name, null);
    }
    public RecruitsPlayerInfo(UUID uuid, String name, @Nullable RecruitsFaction recruitsFaction) {
        this.uuid = uuid;
        this.name = name;
        this.recruitsFaction = recruitsFaction;
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
    public RecruitsFaction getRecruitsTeam(){
        return recruitsFaction;
    }
    @Override
    public String toString() {
        return "{" +
                ", uuid=" + uuid +
                ", name=" + name +
                ", team=" + recruitsFaction +
                '}';
    }


    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("UUID", uuid);
        nbt.putString("Name", name);
        if(recruitsFaction != null){
            nbt.put("RecruitsTeam", this.recruitsFaction.toNBT());
        }

        return nbt;
    }

    public static RecruitsPlayerInfo getFromNBT(CompoundTag nbt) {
        if(nbt == null || nbt.isEmpty()) return null;

        UUID uuid = nbt.getUUID("UUID");
        String name = nbt.getString("Name");
        RecruitsFaction team = RecruitsFaction.fromNBT(nbt.getCompound("RecruitsTeam"));

        return new RecruitsPlayerInfo(uuid, name, team);
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