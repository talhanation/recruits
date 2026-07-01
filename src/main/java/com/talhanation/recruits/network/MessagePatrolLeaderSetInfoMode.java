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

public class MessagePatrolLeaderSetInfoMode implements Message<MessagePatrolLeaderSetInfoMode> {
    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetInfoMode> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetinfomode"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(16.0D),
                v -> v.getUUID().equals(this.recruit) && v.isAlive()
        ).forEach((leader) -> leader.setInfoMode(state));
    }

    public MessagePatrolLeaderSetInfoMode fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readByte();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.state);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetInfoMode> type() {
        return TYPE;
    }
}