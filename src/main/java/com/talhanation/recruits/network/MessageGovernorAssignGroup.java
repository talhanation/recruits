package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
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
        ClaimGovernorData data = GovernorPackets.access(player, claim, true);
        RecruitsClaim recruitsClaim = GovernorManager.getClaim(claim);
        if (data == null || recruitsClaim == null) return;

        RecruitsGroup recruitsGroup = RecruitEvents.recruitsGroupsManager.getGroup(group);
        if (recruitsGroup == null || !player.getUUID().equals(recruitsGroup.getPlayerUUID())) return;

        ServerLevel level = player.serverLevel();
        int free = GovernorManager.getGarrisonCap(recruitsClaim) - GovernorManager.findGarrison(level, recruitsClaim, player.getUUID()).size();

        List<AbstractRecruitEntity> candidates = level.getEntitiesOfClass(AbstractRecruitEntity.class,
                GovernorManager.getClaimBox(level, recruitsClaim, 64),
                r -> r.isAlive() && r.isOwned() && !r.isGarrison()
                        && player.getUUID().equals(r.getOwnerUUID())
                        && group.equals(r.getGroup())
                        && GovernorGarrison.canServe(r));

        int moved = 0;
        for (AbstractRecruitEntity recruit : candidates) {
            if (moved >= free) break;
            GovernorGarrison.joinGarrison(level, recruit, recruitsClaim.getUUID());
            moved++;
        }

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        RecruitEvents.recruitsPlayerUnitManager.broadCastUnitInfoToPlayer(player);
        player.sendSystemMessage(Component.translatable("chat.recruits.governor.assigned", moved, recruitsGroup.getName()));
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
