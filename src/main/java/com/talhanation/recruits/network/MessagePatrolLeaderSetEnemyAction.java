package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetEnemyAction implements RecruitsMessage<MessagePatrolLeaderSetEnemyAction> {

    private UUID recruit;
    private byte action; // 0 = CHARGE, 1 = HOLD, 2 = KEEP_PATROLLING

    public MessagePatrolLeaderSetEnemyAction() {}

    public MessagePatrolLeaderSetEnemyAction(UUID recruit, byte action) {
        this.recruit = recruit;
        this.action = action;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (this.action < AbstractLeaderEntity.EnemyAction.CHARGE.getIndex() || this.action > AbstractLeaderEntity.EnemyAction.KEEP_PATROLLING.getIndex()) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.recruit, 100.0D)
                .ifPresent(leader -> leader.setEnemyAction(AbstractLeaderEntity.EnemyAction.fromIndex(this.action).getIndex()));
    }

    public MessagePatrolLeaderSetEnemyAction fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.action = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.action);
    }
}
