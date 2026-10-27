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
 * The office of a Governor: promotion, appointing him to a claim and removing him from office.
 * A Governor without office is a normal companion of his owner and can be appointed again at any time
 * (claim widget in the Governor screen).
 */
public final class GovernorOffice {

    private GovernorOffice() {
    }

    // ------------------------------------------------------------------ promotion

    /** Any own recruit (not in a garrison) can be promoted; the claim is chosen afterwards. */
    public static boolean canPromote(AbstractRecruitEntity recruit, ServerPlayer player) {
        if (GovernorManager.get() == null) return false;
        if (!player.getUUID().equals(recruit.getOwnerUUID()) || recruit.isGarrison()) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_own", recruit.getName().getString()));
            return false;
        }
        return true;
    }

    /**
     * Replaces the recruit with the new Governor. If he stands in an own claim without Governor,
     * he takes office there right away, otherwise he starts without office.
     */
    public static void setupPromoted(GovernorEntity governor, AbstractRecruitEntity recruit, ServerPlayer player) {
        ServerLevel level = (ServerLevel) recruit.getCommandSenderWorld();
        UUID groupUUID = recruit.getGroup();

        governor.setGroupUUID(null);
        governor.setGovernedClaim(null);
        governor.setHomePos(null);
        governor.setAggroState(0);

        recruit.discard();
        level.addFreshEntity(governor);
        governor.setHoldPos(governor.position());
        governor.setFollowState(3);

        if (groupUUID != null) {
            RecruitEvents.recruitsGroupsManager.removeMember(groupUUID, recruit.getUUID(), level);
            RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        }

        RecruitsClaim claim = GovernorManager.getClaimAt(level, governor.blockPosition());
        if (claim != null && GovernorManager.isClaimOwner(claim, player.getUUID()) && isFree(level, claim, governor)) {
            appoint(governor, claim, player);
        } else {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promoted_no_office", governor.getName().getString()));
        }
    }

    // ------------------------------------------------------------------ appoint / remove

    /**
     * Appoints the Governor to a claim of his owner. Checks run on the server like at the posts:
     * owner of the claim, member of the claim's faction, no other Governor in office there.
     * If he already governs another claim, he leaves it.
     */
    public static boolean appoint(GovernorEntity governor, RecruitsClaim claim, ServerPlayer player) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return false;

        if (!player.getUUID().equals(governor.getOwnerUUID())) return false;
        if (governor.getCommandSenderWorld().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.appoint_dimension", governor.getName().getString()));
            return false;
        }
        if (!GovernorManager.isClaimOwner(claim, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_owner", claim.getName()));
            return false;
        }
        if (claim.getOwnerFaction() == null || !GovernorManager.isFactionMember(claim.getOwnerFactionStringID(), player.getUUID())) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_not_in_faction", claim.getName()));
            return false;
        }
        ServerLevel level = (ServerLevel) governor.getCommandSenderWorld();
        if (!isFree(level, claim, governor)) {
            ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.promote_has_governor", claim.getName(), data.getGovernorName()));
            return false;
        }

        if (governor.getGovernedClaim() != null && !governor.getGovernedClaim().equals(claim.getUUID())) {
            leaveOffice(governor);
        }

        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        data.setGovernor(governor.getUUID(), player.getUUID(), governor.getName().getString());
        data.setGovernorLevel(governor.getXpLevel());
        governor.setGovernedClaim(claim.getUUID());
        governor.setHomePos(GovernorManager.isInClaim(claim, governor.blockPosition()) ? governor.blockPosition() : null);
        governor.setListen(false);
        governor.controller.reset();

        boolean inClaim = GovernorManager.isInClaim(claim, governor.blockPosition());
        player.sendSystemMessage(Component.translatable(inClaim ? "chat.recruits.governor.promoted" : "chat.recruits.governor.appointed_travel",
                governor.getName().getString(), claim.getName()));
        return true;
    }

    /** "X" in the claim widget: the Governor leaves his office, the claim keeps posts, treasury and settings. */
    public static void removeFromOffice(GovernorEntity governor, @Nullable ServerPlayer player) {
        UUID claimId = governor.getGovernedClaim();
        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        leaveOffice(governor);

        governor.setRuling(false);
        governor.setListen(true);
        governor.setFollowState(2);

        if (player != null) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.removed_from_office",
                    governor.getName().getString(), claim != null ? claim.getName() : "?"));
        }
    }

    private static void leaveOffice(GovernorEntity governor) {
        GovernorManager manager = GovernorManager.get();
        UUID claimId = governor.getGovernedClaim();
        if (manager != null && claimId != null) {
            ClaimGovernorData data = manager.getData(claimId);
            if (data != null && governor.getUUID().equals(data.getGovernorId())) {
                data.clearGovernor();
                for (GovernorPost post : data.getPosts()) post.setAssigned(null);
            }
        }
        governor.setGovernedClaim(null);
        governor.setHomePos(null);
        governor.controller.reset();
    }

    /** No Governor registered, or it is this one, or the registered one is dead. Unloaded counts as taken. */
    private static boolean isFree(ServerLevel level, RecruitsClaim claim, GovernorEntity governor) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return false;
        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        if (!data.hasGovernor() || governor.getUUID().equals(data.getGovernorId())) return true;
        Entity existing = level.getEntity(data.getGovernorId());
        if (existing != null && !existing.isAlive()) {
            data.clearGovernor();
            return true;
        }
        return false;
    }
}
