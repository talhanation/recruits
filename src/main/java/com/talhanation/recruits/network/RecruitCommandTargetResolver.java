package com.talhanation.recruits.network;

import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class RecruitCommandTargetResolver {
    private RecruitCommandTargetResolver() {
    }

    static List<AbstractRecruitEntity> resolveGroupTargets(ServerPlayer sender, UUID claimedPlayerUuid, UUID groupUuid, double radius) {
        if (sender == null || claimedPlayerUuid == null || !sender.getUUID().equals(claimedPlayerUuid)) {
            return List.of();
        }
        return sender.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                sender.getBoundingBox().inflate(radius),
                recruit -> recruit.isEffectedByCommand(sender.getUUID(), groupUuid)
        );
    }

    static Optional<AbstractRecruitEntity> resolveOwnedRecruit(ServerPlayer sender, UUID recruitUuid, double radius) {
        return resolveOwnedRecruit(sender, recruitUuid, radius, true);
    }

    static Optional<AbstractRecruitEntity> resolveOwnedRecruit(ServerPlayer sender, UUID recruitUuid, double radius, boolean requireListen) {
        if (sender == null || recruitUuid == null) {
            return Optional.empty();
        }
        return sender.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                sender.getBoundingBox().inflate(radius),
                recruit -> recruit.getUUID().equals(recruitUuid)
                        && (requireListen
                        ? RecruitCommandAuthority.canCommand(sender, recruit)
                        : RecruitCommandAuthority.ownsRecruit(sender, recruit))
        ).stream().findFirst();
    }

    static Optional<AbstractLeaderEntity> resolveOwnedLeader(ServerPlayer sender, UUID leaderUuid, double radius) {
        if (sender == null || leaderUuid == null) {
            return Optional.empty();
        }
        return sender.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                sender.getBoundingBox().inflate(radius),
                leader -> leader.getUUID().equals(leaderUuid) && RecruitCommandAuthority.canCommand(sender, leader)
        ).stream().findFirst();
    }
}
