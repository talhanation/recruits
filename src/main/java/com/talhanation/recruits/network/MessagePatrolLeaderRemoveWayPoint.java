package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;
import com.talhanation.recruits.network.compat.RecruitsPacketDistributor;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderRemoveWayPoint implements RecruitsMessage<MessagePatrolLeaderRemoveWayPoint> {
    private UUID worker;

    public MessagePatrolLeaderRemoveWayPoint() {
    }

    public MessagePatrolLeaderRemoveWayPoint(UUID recruit) {
        this.worker = recruit;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedLeader(player, this.worker, 100.0D)
                .ifPresent((merchant) -> this.removeLastWayPoint(player, merchant));
    }

    private void removeLastWayPoint(ServerPlayer player, AbstractLeaderEntity leaderEntity) {
        if (!leaderEntity.WAYPOINTS.isEmpty()) leaderEntity.WAYPOINTS.pop();
        if (!leaderEntity.WAYPOINT_ITEMS.isEmpty()) leaderEntity.WAYPOINT_ITEMS.pop();

        Main.SIMPLE_CHANNEL.send(RecruitsPacketDistributor.PLAYER.with(() -> player), new MessageToClientUpdateLeaderScreen(leaderEntity.WAYPOINTS, leaderEntity.WAYPOINT_ITEMS, leaderEntity.getArmySize()));
    }

    public MessagePatrolLeaderRemoveWayPoint fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }
}
