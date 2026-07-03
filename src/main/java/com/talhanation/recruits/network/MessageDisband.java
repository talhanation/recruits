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

public class MessageDisband implements Message<MessageDisband> {

    public static final CustomPacketPayload.Type<MessageDisband> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagedisband"));
    private UUID recruit;
    private boolean keepTeam;

    public MessageDisband() {
    }

    public MessageDisband(UUID recruit, boolean keepTeam) {
        this.recruit = recruit;
        this.keepTeam = keepTeam;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16D),
                (recruit) -> recruit.getUUID().equals(this.recruit)
        ).forEach((recruit) -> recruit.disband(((ServerPlayer) context.player()), keepTeam, true));
    }

    public MessageDisband fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.keepTeam = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(keepTeam);
    }

    @Override
    public CustomPacketPayload.Type<MessageDisband> type() {
        return TYPE;
    }
}