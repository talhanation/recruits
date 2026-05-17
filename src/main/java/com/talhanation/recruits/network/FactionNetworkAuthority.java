package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

final class FactionNetworkAuthority {
    static final int MAX_UNIT_COLOR_INDEX = 24;

    private FactionNetworkAuthority() {
    }

    @Nullable
    static RecruitsFaction leaderFaction(ServerPlayer player) {
        if (player == null || player.getTeam() == null) {
            return null;
        }
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(player.getTeam().getName());
        return faction != null && player.getUUID().equals(faction.getTeamLeaderUUID()) ? faction : null;
    }

    static boolean isLeaderOf(ServerPlayer player, String factionId) {
        RecruitsFaction faction = leaderFaction(player);
        return faction != null && faction.getStringID().equals(factionId);
    }

    static boolean isMember(RecruitsFaction faction, RecruitsPlayerInfo playerInfo) {
        return faction != null
                && playerInfo != null
                && faction.getMembers().stream().anyMatch(member -> member.getUUID().equals(playerInfo.getUUID()));
    }

    @Nullable
    static RecruitsPlayerInfo memberByUuid(RecruitsFaction faction, RecruitsPlayerInfo playerInfo) {
        if (faction == null || playerInfo == null) {
            return null;
        }
        return faction.getMembers().stream()
                .filter(member -> member.getUUID().equals(playerInfo.getUUID()))
                .findFirst()
                .orElse(null);
    }
}
