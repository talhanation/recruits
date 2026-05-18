package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageAssignNearbyRecruitsInGroup implements RecruitsMessage<MessageAssignNearbyRecruitsInGroup> {

    private UUID groupUUID;

    public MessageAssignNearbyRecruitsInGroup() {
    }

    public MessageAssignNearbyRecruitsInGroup(UUID group) {
        this.groupUUID = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup newGroup = RecruitCommandAuthority.ownedGroup(player, groupUUID);
        if(newGroup == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.isEffectedByCommand(player.getUUID())
        ).forEach((recruit) -> this.setGroup(recruit, newGroup));

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup(player.serverLevel(), player, newGroup);

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
    }

    public void setGroup(AbstractRecruitEntity recruit, RecruitsGroup group){
        if(recruit.getGroupUUID().isPresent() && recruit.getGroupUUID().get().equals(group)){
            return;
        }

        group.addMember(recruit.getUUID());
        RecruitsGroup oldGroup = RecruitEvents.recruitsGroupsManager.getGroup(recruit.getGroup());
        if(oldGroup != null) oldGroup.removeMember(recruit.getUUID());

        recruit.setGroupUUID(groupUUID);
    }

    public MessageAssignNearbyRecruitsInGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
    }
}
