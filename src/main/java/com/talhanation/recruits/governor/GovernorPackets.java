package com.talhanation.recruits.governor;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.network.CompanionOrders;
import com.talhanation.recruits.network.MessageToClientGovernorData;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.PacketDistributor;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared server-side checks of the Governor packets:
 * the sender owns the claim, is in or next to it, and the claim has a Governor.
 * <p>
 * The exception is a player writing orders for his far Governor at a Messenger: his packets change a copy of the
 * claim's data, which the Messenger then carries to the Governor ({@link GovernorRemoteOrders}).
 */
public final class GovernorPackets {

    private GovernorPackets() {
    }

    @Nullable
    public static ClaimGovernorData access(ServerPlayer player, @Nullable UUID claimId, boolean requireGovernor) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null || player == null) return null;
        GovernorRemoteOrders.Draft draft = CompanionOrders.governorDraft(player, claimId);
        if (draft != null) return draft.data;
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
        if (sendDraft(player, claimId)) return;
        GovernorManager.sendSnapshot(player, claimId, false);
    }

    /** The player is writing orders for this claim's Governor at a Messenger: his screen gets the draft, not the real data. */
    public static boolean sendDraft(ServerPlayer player, @Nullable UUID claimId) {
        GovernorRemoteOrders.Draft draft = CompanionOrders.governorDraft(player, claimId);
        if (draft == null) return false;
        CompoundTag snapshot = GovernorRemoteOrders.snapshot(player, draft);
        if (snapshot != null) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MessageToClientGovernorData(snapshot, false));
        }
        return true;
    }

    /** Things that need the place itself can not be ordered through a Messenger; the player is told. */
    public static boolean refuseInDraft(ServerPlayer player, @Nullable UUID claimId) {
        if (CompanionOrders.governorDraft(player, claimId) == null) return false;
        player.sendSystemMessage(Component.translatable("chat.recruits.governor.only_on_site"));
        return true;
    }
}
