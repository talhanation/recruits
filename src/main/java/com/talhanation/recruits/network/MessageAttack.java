package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAttack implements Message<MessageAttack> {

    public static final CustomPacketPayload.Type<MessageAttack> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageattack"));
    private UUID playerUuid;
    private UUID group;

    public MessageAttack() {
    }

    public MessageAttack(UUID playerUuid, UUID group) {
        this.playerUuid = playerUuid;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(((ServerPlayer) context.player()));
        List<AbstractRecruitEntity> list = serverPlayer.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(100)
        );

        CommandEvents.onAttackCommand(serverPlayer, playerUuid, list, group);
    }

    public MessageAttack fromBytes(RegistryFriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.playerUuid);
        buf.writeUUID(this.group);
    }

    @Override
    public CustomPacketPayload.Type<MessageAttack> type() {
        return TYPE;
    }
}