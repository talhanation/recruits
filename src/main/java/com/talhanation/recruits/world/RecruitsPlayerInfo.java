package com.talhanation.recruits.world;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.scores.PlayerTeam;

import javax.annotation.Nullable;
import java.util.UUID;

public class RecruitsPlayerInfo {
    private UUID uuid;
    private String name;

    @Nullable
    private PlayerTeam playerTeam;

    public RecruitsPlayerInfo(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
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

    public void setPlayerTeam(@Nullable PlayerTeam playerTeam) {
        this.playerTeam = playerTeam;
    }

    @Override
    public String toString() {
        return "{" +
                ", uuid=" + uuid +
                ", name=" + name +
                ", playerTeam=" + playerTeam +
                '}';
    }


}