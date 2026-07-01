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

public class MessageFormationFollowMovement implements Message<MessageFormationFollowMovement> {

    public static final CustomPacketPayload.Type<MessageFormationFollowMovement> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageformationfollowmovement"));
    private UUID player_uuid;

    private UUID group;
    private int formation;

    public MessageFormationFollowMovement(){
    }

    public MessageFormationFollowMovement(UUID player_uuid, UUID group, int formation) {
        this.player_uuid = player_uuid;
        this.group  = group;
        this.formation = formation;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, ((ServerPlayer) context.player()).getBoundingBox().inflate(100));
        list.removeIf(recruit -> !recruit.isEffectedByCommand(this.player_uuid, this.group));

        CommandEvents.applyFormation(formation, list, ((ServerPlayer) context.player()), ((ServerPlayer) context.player()).position());
    }

    public MessageFormationFollowMovement fromBytes(RegistryFriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.group = buf.readUUID();
        this.formation = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeUUID(this.group);
        buf.writeInt(this.formation);
    }


    @Override
    public CustomPacketPayload.Type<MessageFormationFollowMovement> type() {
        return TYPE;
    }
}