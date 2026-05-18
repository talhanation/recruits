package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetInfoMode implements RecruitsMessage<MessagePatrolLeaderSetInfoMode> {
    private UUID recruit;
    private byte state;

    public MessagePatrolLeaderSetInfoMode() {
    }

    public MessagePatrolLeaderSetInfoMode(UUID recruit, byte state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (state < AbstractLeaderEntity.InfoMode.ALL.getIndex() || state > AbstractLeaderEntity.InfoMode.HOSTILE.getIndex()) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.recruit, 16.0D)
                .ifPresent((leader) -> leader.setInfoMode(state));
    }

    public MessagePatrolLeaderSetInfoMode fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.state);
    }
}
