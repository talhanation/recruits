package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetPatrolState implements RecruitsMessage<MessagePatrolLeaderSetPatrolState> {
    private UUID recruit;
    private byte state;

    public MessagePatrolLeaderSetPatrolState() {
    }

    public MessagePatrolLeaderSetPatrolState(UUID recruit, byte state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!isValidPatrolState(this.state)) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.recruit, 64.0D)
                .ifPresent(this::setState);
    }

    private static boolean isValidPatrolState(byte state) {
        return state == AbstractLeaderEntity.State.PATROLLING.getIndex()
                || state == AbstractLeaderEntity.State.PAUSED.getIndex()
                || state == AbstractLeaderEntity.State.STOPPED.getIndex();
    }

    private void setState(AbstractLeaderEntity leader) {
        AbstractLeaderEntity.State leaderState = AbstractLeaderEntity.State.fromIndex(state);
        switch (leaderState) {
            case PATROLLING -> leader.setFollowState(0);
            case STOPPED, PAUSED -> leader.setFollowState(1);
        }
        leader.setPatrolState(leaderState);
        leader.currentWaypoint = null;
    }

    public MessagePatrolLeaderSetPatrolState fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.state);
    }
}
