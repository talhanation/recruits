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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageStrategicFire implements Message<MessageStrategicFire> {

    public static final CustomPacketPayload.Type<MessageStrategicFire> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagestrategicfire"));
    private UUID player;
    private UUID group;
    private boolean should;

    public MessageStrategicFire() {
    }

    public MessageStrategicFire(UUID player, UUID group, boolean should) {
        this.player = player;
        this.group = group;
        this.should = should;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(((ServerPlayer) context.player()));
        serverPlayer.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(200)
        ).forEach((recruit) ->
                CommandEvents.onStrategicFireCommand(
                        serverPlayer,
                        this.player,
                        recruit,
                        group,
                        should
                )
        );
    }

    public MessageStrategicFire fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.should = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.should);
    }

    @Override
    public CustomPacketPayload.Type<MessageStrategicFire> type() {
        return TYPE;
    }
}