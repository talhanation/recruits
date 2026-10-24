package com.talhanation.recruits.governor;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Promotion of a recruit to Governor: only the owner of the claim the recruit stands in,
 * only while he is in the claim's faction, and only if the claim has no Governor yet.
 */
public final class GovernorPromotion {

    private GovernorPromotion() {
    }

    @Nullable
    public static RecruitsClaim validate(AbstractRecruitEntity recruit, ServerPlayer player) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return null;

        if (!player.getUUID().equals(recruit.getOwnerUUID()) || recruit.isGarrison()) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_own", recruit.getName().getString()));
            return null;
        }
        if (recruit.getCommandSenderWorld().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_no_claim"));
            return null;
        }

        RecruitsClaim claim = GovernorManager.getClaimAt(recruit.getCommandSenderWorld(), recruit.blockPosition());
        if (claim == null) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_no_claim"));
            return null;
        }
        if (!GovernorManager.isClaimOwner(claim, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_owner", claim.getName()));
            return null;
        }
        if (claim.getOwnerFaction() == null || !GovernorManager.isFactionMember(claim.getOwnerFactionStringID(), player.getUUID())) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_in_faction", claim.getName()));
            return null;
        }

        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        if (data.hasGovernor()) {
            Entity existing = ((ServerLevel) recruit.getCommandSenderWorld()).getEntity(data.getGovernorId());
            if (existing == null || existing.isAlive()) {
                player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_has_governor", claim.getName(), data.getGovernorName()));
                return null;
            }
            data.clearGovernor();
        }
        return claim;
    }

    public static void setup(GovernorEntity governor, AbstractRecruitEntity recruit, RecruitsClaim claim, ServerPlayer player) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return;
        ServerLevel level = (ServerLevel) recruit.getCommandSenderWorld();

        UUID groupUUID = recruit.getGroup();

        governor.setGroupUUID(null);
        governor.setGovernedClaim(claim.getUUID());
        governor.setHomePos(recruit.blockPosition());
        governor.setListen(false);
        governor.setAggroState(0);
        governor.setHoldPos(recruit.position());
        governor.setFollowState(3);

        recruit.discard();
        level.addFreshEntity(governor);

        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        data.setGovernor(governor.getUUID(), player.getUUID(), governor.getName().getString());

        if (groupUUID != null) {
            RecruitEvents.recruitsGroupsManager.removeMember(groupUUID, recruit.getUUID(), level);
            RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        }

        player.sendSystemMessage(Component.translatable("chat.recruits.governor.promoted", governor.getName().getString(), claim.getName()));
    }
}
