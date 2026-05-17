package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageAnswerMessenger implements Message<MessageAnswerMessenger> {

    private UUID recruit;
    public MessageAnswerMessenger() {
    }
    public MessageAnswerMessenger(UUID recruit) {
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
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
