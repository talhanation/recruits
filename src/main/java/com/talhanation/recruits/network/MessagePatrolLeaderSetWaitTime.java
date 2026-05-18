package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetWaitTime implements RecruitsMessage<MessagePatrolLeaderSetWaitTime> {

    private UUID recruit;
    private int time;

    public MessagePatrolLeaderSetWaitTime() {
    }

    public MessagePatrolLeaderSetWaitTime(UUID recruit, int time) {
        this.recruit = recruit;
        this.time = time;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.recruit, 100.0D)
                .ifPresent((leader) -> leader.setWaitTimeInMin(Math.max(0, this.time)));
    }

    public MessagePatrolLeaderSetWaitTime fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.time = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeInt(this.time);
    }
}
