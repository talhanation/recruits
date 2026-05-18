package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetPatrollingSpeed implements RecruitsMessage<MessagePatrolLeaderSetPatrollingSpeed> {

    private UUID recruit;
    private byte speed; // 0 = SLOW, 1 = NORMAL, 2 = FAST

    public MessagePatrolLeaderSetPatrollingSpeed() {}

    public MessagePatrolLeaderSetPatrollingSpeed(UUID recruit, byte speed) {
        this.recruit = recruit;
        this.speed = speed;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (this.speed < AbstractLeaderEntity.PatrolSpeed.SLOW.getIndex() || this.speed > AbstractLeaderEntity.PatrolSpeed.FAST.getIndex()) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.recruit, 100.0D)
                .ifPresent(leader -> leader.setPatrolSpeed(AbstractLeaderEntity.PatrolSpeed.fromIndex(this.speed).getIndex()));
    }

    public MessagePatrolLeaderSetPatrollingSpeed fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.speed = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.speed);
    }
}
