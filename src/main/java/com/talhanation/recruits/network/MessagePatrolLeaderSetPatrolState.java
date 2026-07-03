package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetPatrolState implements Message<MessagePatrolLeaderSetPatrolState> {
    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetPatrolState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetpatrolstate"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(64.0D),
                v -> v.getUUID().equals(this.recruit) && v.isAlive()
        ).forEach(this::setState);
    }

    public void setState(AbstractLeaderEntity leader) {
        AbstractLeaderEntity.State leaderState = AbstractLeaderEntity.State.fromIndex(state);
        switch (leaderState) {
            case PATROLLING -> leader.setFollowState(0);
            case STOPPED, PAUSED -> leader.setFollowState(1);
        }
        leader.setPatrolState(leaderState);
        leader.currentWaypoint = null;
    }

    public MessagePatrolLeaderSetPatrolState fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readByte();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.state);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetPatrolState> type() {
        return TYPE;
    }
}