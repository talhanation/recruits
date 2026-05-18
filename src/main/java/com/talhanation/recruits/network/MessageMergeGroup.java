package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageMergeGroup implements RecruitsMessage<MessageMergeGroup> {

    private UUID groupUUID;
    private UUID mergeUUID;

    public MessageMergeGroup() {
    }

    public MessageMergeGroup(UUID mergeUUID, UUID groupUUID) {
        this.mergeUUID = mergeUUID;
        this.groupUUID = groupUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup groupToMerge = RecruitEvents.recruitsGroupsManager.getGroup(mergeUUID);
        RecruitsGroup baseGroup = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToMerge == null || baseGroup == null) return;

        RecruitEvents.recruitsGroupsManager.mergeGroups(groupToMerge, baseGroup, player.serverLevel());
    }

    public MessageMergeGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        this.mergeUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
        buf.writeUUID(mergeUUID);
    }
}