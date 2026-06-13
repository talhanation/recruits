package com.talhanation.recruits.client.gui.worldmap.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class WorldMapStorageId {
    private static UUID serverWorldId;

    private WorldMapStorageId() {}

    public static void setServerWorldId(UUID worldId) {
        serverWorldId = worldId;
    }

    public static void clearServerWorldId() {
        serverWorldId = null;
    }

    public static String detectCurrent() {
        return detect(Minecraft.getInstance().level);
    }

    public static String detect(@Nullable Level level) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String dimension = level == null ? "unknown" : level.dimension().location().toString();
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
                if (serverWorldId != null) {
                    return stableId("mp", serverData.ip + "|world=" + serverWorldId + "|dim=" + dimension);
                }
                return stableId("mp_pending", serverData.ip + "|dim=" + dimension);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    static boolean isTemporary(String storageId) {
        return "unknown".equals(storageId) || storageId.startsWith("mp_pending_");
    }

    private static String stableId(String prefix, String rawId) {
        return prefix + "_" + UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));
    }
}
