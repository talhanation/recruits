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


public class MessagePatrolLeaderSetCycle implements Message<MessagePatrolLeaderSetCycle> {

    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetCycle> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetcycle"));
    private UUID recruit;
    private boolean cycle;

    public MessagePatrolLeaderSetCycle() {
    }

    public MessagePatrolLeaderSetCycle(UUID recruit, boolean cycle) {
        this.recruit = recruit;
        this.cycle = cycle;
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
        ).forEach(leader -> leader.setCycle(this.cycle));
    }

    public MessagePatrolLeaderSetCycle fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.cycle = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.cycle);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetCycle> type() {
        return TYPE;
    }
}
