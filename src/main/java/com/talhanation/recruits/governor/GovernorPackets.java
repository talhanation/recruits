package com.talhanation.recruits.governor;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared server-side checks of the Governor packets:
 * the sender owns the claim, is in or next to it, and the claim has a Governor.
 */
public final class GovernorPackets {

    private GovernorPackets() {
    }

    @Nullable
    public static ClaimGovernorData access(ServerPlayer player, @Nullable UUID claimId, boolean requireGovernor) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null || player == null) return null;
        RecruitsClaim claim = GovernorManager.getManageableClaim(player, claimId);
        if (claim == null) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.no_access"));
            return null;
        }
        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        if (requireGovernor && !data.hasGovernor()) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.no_governor", claim.getName()));
            return null;
        }
        return data;
    }

    public static void sendUpdate(ServerPlayer player, UUID claimId) {
        GovernorManager.sendSnapshot(player, claimId, false);
    }
}
