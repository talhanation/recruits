package com.talhanation.recruits.world;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecruitUnitManager {
    private Map<UUID, Integer> recruitCountMap = new HashMap<>();

    public void load(ServerLevel level) {
        RecruitPlayerUnitSaveData data = RecruitPlayerUnitSaveData.get(level);

        recruitCountMap = data.getRecruitCountMap();
    }

    public void save(ServerLevel level) {
        RecruitPlayerUnitSaveData data = RecruitPlayerUnitSaveData.get(level);

        for (Map.Entry<UUID, Integer> entry : recruitCountMap.entrySet()) {
            data.setRecruitCount(entry.getKey(), entry.getValue());
        }

        data.setDirty();
    }

    public int getRecruitCount(UUID playerUUID) {
        return recruitCountMap.getOrDefault(playerUUID, 0);
    }

    public void setRecruitCount(UUID playerUUID, int count) {
        recruitCountMap.put(playerUUID, count);
    }

    public void addRecruits(UUID playerUUID, int count) {
        recruitCountMap.put(playerUUID, getRecruitCount(playerUUID) + count);
    }

    public void removeRecruits(UUID playerUUID, int count) {
        recruitCountMap.put(playerUUID, Math.max(getRecruitCount(playerUUID) - count, 0));
    }

    public boolean canPlayerRecruit(UUID playerUUID) {
        int current = getRecruitCount(playerUUID);

        return current < RecruitsServerConfig.MaxRecruitsForPlayer.get();
    }
}
