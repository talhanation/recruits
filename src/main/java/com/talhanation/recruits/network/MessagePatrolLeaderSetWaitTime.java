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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetWaitTime implements Message<MessagePatrolLeaderSetWaitTime> {

    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetWaitTime> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetwaittime"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                (leader) -> leader.getUUID().equals(this.recruit)
        ).forEach((leader) -> leader.setWaitTimeInMin(this.time));
    }

    public MessagePatrolLeaderSetWaitTime fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.time = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeInt(this.time);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetWaitTime> type() {
        return TYPE;
    }
}