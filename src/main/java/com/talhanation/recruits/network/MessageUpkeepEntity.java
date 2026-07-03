package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageUpkeepEntity implements Message<MessageUpkeepEntity> {

    public static final CustomPacketPayload.Type<MessageUpkeepEntity> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageupkeepentity"));
    private UUID player_uuid;
    private UUID target;
    private UUID group;

    public MessageUpkeepEntity() {
    }

    public MessageUpkeepEntity(UUID player_uuid, UUID target, UUID group) {
        this.player_uuid = player_uuid;
        this.target = target;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100)
        ).forEach(recruit -> CommandEvents.onUpkeepCommand(player_uuid, recruit, group, true, target, null));
    }

    public MessageUpkeepEntity fromBytes(RegistryFriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(player_uuid);
        buf.writeUUID(target);
        buf.writeUUID(group);
    }

    @Override
    public CustomPacketPayload.Type<MessageUpkeepEntity> type() {
        return TYPE;
    }
}