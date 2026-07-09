package com.talhanation.recruits.command;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Server-side authority checks for recruit command and group mutations.
 */
public final class RecruitCommandAuthority {
    private RecruitCommandAuthority() {
    }

    public static boolean canCommand(ServerPlayer player, AbstractRecruitEntity recruit) {
        return player != null && recruit != null && recruit.isEffectedByCommand(player.getUUID());
    }

    public static boolean ownsRecruit(ServerPlayer player, AbstractRecruitEntity recruit) {
        return player != null
                && recruit != null
                && recruit.isOwned()
                && recruit.isAlive()
                && player.getUUID().equals(recruit.getOwnerUUID());
    }

    public static boolean ownsGroup(ServerPlayer player, @Nullable UUID groupUuid) {
        return ownedGroup(player, groupUuid) != null;
    }

    @Nullable
    public static RecruitsGroup ownedGroup(ServerPlayer player, @Nullable UUID groupUuid) {
        if (player == null || groupUuid == null || RecruitEvents.recruitsGroupsManager == null) {
            return null;
        }
        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUuid);
        return group != null && player.getUUID().equals(group.getPlayerUUID()) ? group : null;
    }
}
