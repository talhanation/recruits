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
import java.util.Objects;
import java.util.UUID;

public class MessageUpkeepPos implements Message<MessageUpkeepPos> {

    public static final CustomPacketPayload.Type<MessageUpkeepPos> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageupkeeppos"));
    private UUID player;
    private UUID group;
    private BlockPos pos;

    public MessageUpkeepPos() {
    }

    public MessageUpkeepPos(UUID player, UUID group, BlockPos pos) {
        this.player = player;
        this.group = group;
        this.pos = pos;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100)
        ).forEach((recruit) -> CommandEvents.onUpkeepCommand(
                this.player,
                recruit,
                group,
                false,
                null,
                pos)
        );
    }

    public MessageUpkeepPos fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.pos = buf.readBlockPos();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBlockPos(this.pos);
    }

    @Override
    public CustomPacketPayload.Type<MessageUpkeepPos> type() {
        return TYPE;
    }
}