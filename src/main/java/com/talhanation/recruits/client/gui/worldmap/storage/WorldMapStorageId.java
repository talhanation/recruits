package com.talhanation.recruits.client.gui.worldmap.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class WorldMapStorageId {
    private WorldMapStorageId() {}

    static String detect(Level level) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String dimension = level.dimension().location().toString();
            if (mc.getSingleplayerServer() != null) {
                var server = mc.getSingleplayerServer();
                java.nio.file.Path root =
                        server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                long seed = 0L;
                try {
                    seed = server.overworld().getSeed();
                } catch (Exception ignored) {
                }
                return stableId("sp", root + "|seed=" + seed + "|dim=" + dimension);
            }
            ServerData serverData = mc.getCurrentServer();
            if (serverData != null && serverData.ip != null && !serverData.ip.isEmpty()) {
                return stableId("mp", serverData.ip + "|dim=" + dimension);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static String stableId(String prefix, String rawId) {
        return prefix + "_" + UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
    }
}
