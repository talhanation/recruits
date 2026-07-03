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

public class MessageClearTarget implements Message<MessageClearTarget> {
    public static final CustomPacketPayload.Type<MessageClearTarget> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagecleartarget"));
    private UUID uuid;
    private UUID group;

    public MessageClearTarget(){
    }

    public MessageClearTarget(UUID uuid, UUID group) {
        this.uuid = uuid;
        this.group = group;

    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        List<AbstractRecruitEntity> list = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100));
        for (AbstractRecruitEntity recruits : list) {
            CommandEvents.onClearTargetButton(uuid, recruits, group);
        }
    }
    public MessageClearTarget fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(group);
    }


    @Override
    public CustomPacketPayload.Type<MessageClearTarget> type() {
        return TYPE;
    }
}

