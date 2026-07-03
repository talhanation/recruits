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

public class MessagePatrolLeaderSetEnemyAction implements Message<MessagePatrolLeaderSetEnemyAction> {

    public static final CustomPacketPayload.Type<MessagePatrolLeaderSetEnemyAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleadersetenemyaction"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                leader -> leader.getUUID().equals(this.recruit) && leader.isAlive()
        ).forEach(leader -> leader.setEnemyAction(this.action));
    }

    public MessagePatrolLeaderSetEnemyAction fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.action = buf.readByte();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.action);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderSetEnemyAction> type() {
        return TYPE;
    }
}
