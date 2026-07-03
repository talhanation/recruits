package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageListen implements Message<MessageListen> {

    public static final CustomPacketPayload.Type<MessageListen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagelisten"));
    private boolean bool;
    private UUID uuid;

    public MessageListen() {
    }

    public MessageListen(boolean bool, UUID uuid) {
        this.bool = bool;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> recruit.setListen(bool));
    }

    public MessageListen fromBytes(RegistryFriendlyByteBuf buf) {
        this.bool = buf.readBoolean();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(bool);
        buf.writeUUID(uuid);
    }

    @Override
    public CustomPacketPayload.Type<MessageListen> type() {
        return TYPE;
    }
}