package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → Server: assigns a group UUID to a leader entity so the
 * subsequent {@link MessageAssignGroupToCompanion} knows which group to use.
 */
public class MessageSetLeaderGroup implements Message<MessageSetLeaderGroup>, CompanionOrder<AbstractLeaderEntity> {

    private UUID leaderUUID;
    @Nullable
    private UUID groupUUID;

    public MessageSetLeaderGroup() {}

    public MessageSetLeaderGroup(UUID leaderUUID, @Nullable UUID groupUUID) {
        this.leaderUUID = leaderUUID;
        this.groupUUID  = groupUUID;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.leaderUUID;
    }

    @Override
    public Class<AbstractLeaderEntity> getCompanionType() {
        return AbstractLeaderEntity.class;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public void applyTo(AbstractLeaderEntity leader, @Nullable ServerPlayer sender) {
        if (groupUUID == null) {
            leader.setGroupUUID(null);
            return;
        }
        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if (!CompanionOrders.canManageGroup(leader, sender, group)) return;
        leader.setGroupUUID(group.getUUID());
        ServerPlayer player = CompanionOrders.playerFor(leader, sender);
        if (player != null) RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
    }

    @Override
    public MessageSetLeaderGroup fromBytes(FriendlyByteBuf buf) {
        this.leaderUUID = buf.readUUID();
        boolean hasGroup = buf.readBoolean();
        this.groupUUID  = hasGroup ? buf.readUUID() : null;
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.leaderUUID);
        buf.writeBoolean(this.groupUUID != null);
        if (this.groupUUID != null) buf.writeUUID(this.groupUUID);
    }
}
