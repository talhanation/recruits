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

public class MessagePatrolLeaderSetPatrollingSpeed implements Message<MessagePatrolLeaderSetPatrollingSpeed> {

    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetPatrollingSpeed> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetpatrollingspeed"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100.0D),
                recruit -> recruit.getUUID().equals(this.recruit)
        ).forEach(leader -> leader.setPatrolSpeed(this.speed));
    }

    public MessagePatrolLeaderSetPatrollingSpeed fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.speed = buf.readByte();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.speed);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetPatrollingSpeed> type() {
        return TYPE;
    }
}
