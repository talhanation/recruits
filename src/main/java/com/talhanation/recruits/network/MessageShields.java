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

public class MessageShields implements Message<MessageShields> {

    public static final CustomPacketPayload.Type<MessageShields> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageshields"));
    private UUID player;
    private UUID group;
    private boolean should;

    public MessageShields() {
    }

    public MessageShields(UUID player, UUID group, boolean shields) {
        this.player = player;
        this.group = group;
        this.should = shields;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100),
                (recruit) -> recruit.isEffectedByCommand(this.player, group)
        ).forEach((recruit) -> CommandEvents.onShieldsCommand(
                        player,
                        this.player,
                        recruit,
                        group,
                        should
                )
        );
    }

    public MessageShields fromBytes(RegistryFriendlyByteBuf buf) {
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
    public CustomPacketPayload.Type<MessageShields> type() {
        return TYPE;
    }
}