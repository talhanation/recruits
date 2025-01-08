package com.talhanation.recruits.world;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecruitsPlayerUnitManager {
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

    public boolean canPlayerRecruit(String stringId, UUID playerUUID) {
        RecruitsTeam recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(stringId);

        int currentRecruitCount = getRecruitCount(playerUUID);
        int maxRecruitCount = 0;

        if (recruitsTeam == null) {
            maxRecruitCount = RecruitsServerConfig.MaxRecruitsForPlayer.get();
        } else {

            if (playerUUID.equals(recruitsTeam.getTeamLeaderUUID())) {

                maxRecruitCount = recruitsTeam.maxNPCs;
            } else {
                maxRecruitCount = recruitsTeam.getMaxNPCsPerPlayer();
            }

            if (recruitsTeam.npcs >= recruitsTeam.maxNPCs) {
                return false;
            }
        }

        return currentRecruitCount < maxRecruitCount;
    }
}
