package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageDismountGui implements Message<MessageDismountGui> {

    public static final CustomPacketPayload.Type<MessageDismountGui> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagedismountgui"));
    private UUID uuid;
    private UUID player;

    public MessageDismountGui() {
    }

    public MessageDismountGui(UUID player, UUID uuid) {
        this.player = player;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(((ServerPlayer) context.player()));
        serverPlayer.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(16.0D),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> CommandEvents.onDismountButton(player, recruit, null));
    }

    public MessageDismountGui fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.player = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(player);
    }

    @Override
    public CustomPacketPayload.Type<MessageDismountGui> type() {
        return TYPE;
    }
}