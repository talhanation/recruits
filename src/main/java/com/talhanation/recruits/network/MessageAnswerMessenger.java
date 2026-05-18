package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageAnswerMessenger implements RecruitsMessage<MessageAnswerMessenger> {

    private UUID recruit;
    public MessageAnswerMessenger() {
    }
    public MessageAnswerMessenger(UUID recruit) {
        this.recruit = recruit;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        for (MessengerEntity messenger : player.getCommandSenderWorld().getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                messenger -> messenger.getUUID().equals(this.recruit) && canAnswer(player, messenger)
        )){
            messenger.teleportWaitTimer = 100;
            player.sendSystemMessage(messenger.MESSENGER_INFO_ON_MY_WAY());
            messenger.giveDeliverItem(player);

            messenger.setMessengerState(MessengerEntity.MessengerState.TELEPORT_BACK);
            break;
        }

    }

    private static boolean canAnswer(ServerPlayer player, MessengerEntity messenger) {
        return !messenger.isTreatyMessenger()
                && messenger.getTargetPlayerInfo() != null
                && player.getUUID().equals(messenger.getTargetPlayerInfo().getUUID());
    }

    public MessageAnswerMessenger fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }
}
