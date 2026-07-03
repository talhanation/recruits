package com.talhanation.recruits.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class GameProfileUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static ResourceLocation getSkin(UUID uuid) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return DefaultPlayerSkin.get(uuid).texture();
        }
        PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
        if (playerInfo == null) {
            return DefaultPlayerSkin.get(uuid).texture();
        }
        return playerInfo.getSkin().texture();
    }

}