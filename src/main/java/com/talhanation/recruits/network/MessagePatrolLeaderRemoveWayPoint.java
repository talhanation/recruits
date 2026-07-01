package com.talhanation.recruits.network;
import de.maxhenkel.corelib.net.NetUtils;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderRemoveWayPoint implements Message<MessagePatrolLeaderRemoveWayPoint> {
    public static final CustomPacketPayload.Type<MessagePatrolLeaderRemoveWayPoint> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleaderremovewaypoint"));
    private UUID worker;

    public MessagePatrolLeaderRemoveWayPoint() {
    }

    public MessagePatrolLeaderRemoveWayPoint(UUID recruit) {
        this.worker = recruit;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                v -> v.getUUID().equals(this.worker) && v.isAlive()
        ).forEach((merchant) -> this.removeLastWayPoint(player, merchant));
    }

    private void removeLastWayPoint(ServerPlayer player, AbstractLeaderEntity leaderEntity) {
        if (!leaderEntity.WAYPOINTS.isEmpty()) leaderEntity.WAYPOINTS.pop();
        if (!leaderEntity.WAYPOINT_ITEMS.isEmpty()) leaderEntity.WAYPOINT_ITEMS.pop();

        NetUtils.sendTo(player, new MessageToClientUpdateLeaderScreen(leaderEntity.WAYPOINTS, leaderEntity.WAYPOINT_ITEMS, leaderEntity.getArmySize()));
    }

    public MessagePatrolLeaderRemoveWayPoint fromBytes(RegistryFriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderRemoveWayPoint> type() {
        return TYPE;
    }
}
