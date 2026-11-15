package com.talhanation.recruits.api.governor;

import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Entry points for modules outside the Governor's own hooks: packets of a module tab and
 * events of the addon (e.g. a building was finished).
 */
public final class GovernorAccess {

    private GovernorAccess() {
    }

    /**
     * Server side check for a packet of a module tab: the player owns the claim, stands in it or next to it,
     * and the claim has a Governor. Sends the player a message and returns null if not.
     */
    @Nullable
    public static GovernorContext forPlayer(ServerPlayer player, @Nullable UUID claimId, ResourceLocation moduleId) {
        ClaimGovernorData data = GovernorPackets.access(player, claimId, true);
        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        if (data == null || claim == null) return null;
        return create(player.server.overworld(), claim, data, moduleId);
    }

    /** Context of a claim that has a Governor, null if it has none. Claims exist only in the overworld. */
    @Nullable
    public static GovernorContext forClaim(ServerLevel overworld, @Nullable UUID claimId, ResourceLocation moduleId) {
        GovernorManager manager = GovernorManager.get();
        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        if (manager == null || claim == null) return null;
        ClaimGovernorData data = manager.getData(claim.getUUID());
        if (data == null || !data.hasGovernor()) return null;
        return create(overworld, claim, data, moduleId);
    }

    /** Sends the player a fresh snapshot of the claim, so an open Governor screen shows the change. */
    public static void sendUpdate(ServerPlayer player, UUID claimId) {
        GovernorPackets.sendUpdate(player, claimId);
    }

    private static GovernorContext create(ServerLevel overworld, RecruitsClaim claim, ClaimGovernorData data, ResourceLocation moduleId) {
        GovernorEntity governor = null;
        if (data.getGovernorId() != null && overworld.getEntity(data.getGovernorId()) instanceof GovernorEntity g && g.isAlive()) {
            governor = g;
        }
        return new GovernorContext(overworld, claim, data, moduleId, governor, governor != null ? governor.controller : null);
    }
}
