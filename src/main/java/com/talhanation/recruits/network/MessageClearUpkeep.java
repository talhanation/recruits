package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageClearUpkeep implements Message<MessageClearUpkeep> {
    public static final CustomPacketPayload.Type<MessageClearUpkeep> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageclearupkeep"));
    private UUID uuid;
    private UUID group;

    public MessageClearUpkeep() {
    }

    public MessageClearUpkeep(UUID uuid, UUID group) {
        this.uuid = uuid;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100)
        ).forEach(
                (recruit) -> CommandEvents.onClearUpkeepButton(uuid, recruit, group)
        );
    }

    public MessageClearUpkeep fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(group);
    }

    @Override
    public CustomPacketPayload.Type<MessageClearUpkeep> type() {
        return TYPE;
    }
}

