package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.MessengerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAnswerMessenger implements Message<MessageAnswerMessenger> {

    public static final CustomPacketPayload.Type<MessageAnswerMessenger> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageanswermessenger"));
    private UUID recruit;
    public MessageAnswerMessenger() {
    }
    public MessageAnswerMessenger(UUID recruit) {
        this.recruit = recruit;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        List<MessengerEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(
                MessengerEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(16D)
        );
        for (MessengerEntity messenger : list){

            if (messenger.getUUID().equals(this.recruit)){

                messenger.teleportWaitTimer = 100;
                ((ServerPlayer) context.player()).sendSystemMessage(messenger.MESSENGER_INFO_ON_MY_WAY());
                messenger.giveDeliverItem(player);

                messenger.setMessengerState(MessengerEntity.MessengerState.TELEPORT_BACK);
            }
        }

    }
    public MessageAnswerMessenger fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }

    @Override
    public CustomPacketPayload.Type<MessageAnswerMessenger> type() {
        return TYPE;
    }
}
