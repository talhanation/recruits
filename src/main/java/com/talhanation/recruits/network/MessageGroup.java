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

public class MessageGroup implements RecruitsMessage<MessageGroup> {

    private UUID groupUUID;
    private UUID recruitUUID;

    public MessageGroup() {
    }

    public MessageGroup(UUID groupUUID, UUID recruitUUID) {
        this.groupUUID = groupUUID;
        this.recruitUUID = recruitUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruitUUID, 100D)
                .ifPresent((recruit) -> this.setGroup(recruit, player, groupUUID));
    }

    public void setGroup(AbstractRecruitEntity recruit, ServerPlayer player , UUID groupUUID){
        RecruitsGroup oldGroup = RecruitEvents.recruitsGroupsManager.getGroup(recruit.getGroup());
        RecruitsGroup newGroup = RecruitCommandAuthority.ownedGroup(player, groupUUID);
        if (newGroup == null) return;
        if(oldGroup != null && newGroup != null && oldGroup.getUUID().equals(newGroup.getUUID())) return;

        if(oldGroup != null) RecruitEvents.recruitsGroupsManager.removeMember(oldGroup.getUUID(), recruit.getUUID(), player.serverLevel());
        if(newGroup != null) RecruitEvents.recruitsGroupsManager.addMember(newGroup.getUUID(), recruit.getUUID(), player.serverLevel());

        recruit.setGroupUUID(newGroup.getUUID());
    }

    public MessageGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        this.recruitUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
        buf.writeUUID(recruitUUID);
    }
}
