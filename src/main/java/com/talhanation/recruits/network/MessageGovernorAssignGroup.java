package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.api.governor.GovernorAccess;
import com.talhanation.recruits.api.governor.GovernorContext;
import com.talhanation.recruits.api.governor.GovernorModules;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.governor.GovernorPackets;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hands the recruits of one of the player's groups over to the garrison of the claim (up to the cap). */
public class MessageGovernorAssignGroup implements Message<MessageGovernorAssignGroup> {

    private UUID claim;
    private UUID group;

    public MessageGovernorAssignGroup() {
    }

    public MessageGovernorAssignGroup(UUID claim, UUID group) {
        this.claim = claim;
        this.group = group;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        // the group has to stand in the claim: not something a Messenger can carry
        if (GovernorPackets.refuseInDraft(player, claim)) return;
        ClaimGovernorData data = GovernorPackets.access(player, claim, true);
        RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
        if (data == null || recruitsClaim == null) return;

        RecruitsGroup recruitsGroup = RecruitEvents.recruitsGroupsManager.getGroup(group);
        if (recruitsGroup == null || !player.getUUID().equals(recruitsGroup.getPlayerUUID())) return;

        ServerLevel level = player.serverLevel();
        int free = GovernorManager.getGarrisonCap(recruitsClaim) - GovernorManager.countGarrison(level, recruitsClaim, player.getUUID());

        List<AbstractRecruitEntity> members = level.getEntitiesOfClass(AbstractRecruitEntity.class,
                GovernorManager.getClaimBox(level, recruitsClaim, 64),
                r -> r.isAlive() && r.isOwned() && !r.isGarrison()
                        && player.getUUID().equals(r.getOwnerUUID())
                        && group.equals(r.getGroup()));
        List<AbstractRecruitEntity> candidates = new ArrayList<>();
        List<AbstractRecruitEntity> others = new ArrayList<>();
        for (AbstractRecruitEntity member : members) (GovernorGarrison.canServe(member) ? candidates : others).add(member);

        int moved = 0;
        for (AbstractRecruitEntity recruit : candidates) {
            if (moved >= free) break;
            GovernorGarrison.joinGarrison(level, recruit, recruitsClaim.getUUID());
            moved++;
        }
        // units that can not serve (workers): a module of an addon may take them, then they join the garrison too
        int handed = 0;
        for (AbstractRecruitEntity unit : others) {
            if (moved + handed >= free) break;
            boolean[] taken = {false};
            GovernorModules.forEach("onUnitHandedOver", module -> {
                if (taken[0]) return;
                GovernorContext ctx = GovernorAccess.forClaim(player.server.overworld(), recruitsClaim.getUUID(), module.id());
                if (ctx != null && module.onUnitHandedOver(ctx, unit)) taken[0] = true;
            });
            if (taken[0]) {
                GovernorGarrison.joinGarrison(level, unit, recruitsClaim.getUUID());
                handed++;
            }
        }

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        RecruitEvents.recruitsPlayerUnitManager.broadCastUnitInfoToPlayer(player);
        player.sendSystemMessage(Component.translatable("chat.recruits.governor.assigned", moved + handed, recruitsGroup.getName()));
        if (moved < candidates.size()) {
            player.sendSystemMessage(Component.translatable("chat.recruits.governor.garrison_full", GovernorManager.getGarrisonCap(recruitsClaim)));
        }
        GovernorPackets.sendUpdate(player, claim);
    }

    @Override
    public MessageGovernorAssignGroup fromBytes(FriendlyByteBuf buf) {
        this.claim = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(claim);
        buf.writeUUID(group);
    }
}
