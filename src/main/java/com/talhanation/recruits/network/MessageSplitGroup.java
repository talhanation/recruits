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

public class MessageSplitGroup implements RecruitsMessage<MessageSplitGroup> {

    private UUID groupUUID;

    public MessageSplitGroup() {
    }

    public MessageSplitGroup(UUID groupUUID) {
        this.groupUUID = groupUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup groupToSplit = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToSplit == null) return;

        RecruitEvents.recruitsGroupsManager.splitGroup(groupToSplit, player.serverLevel());
    }

    public MessageSplitGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
    }
}