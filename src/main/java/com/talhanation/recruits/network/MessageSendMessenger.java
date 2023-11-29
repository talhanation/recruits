package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageSendMessenger implements Message<MessageSendMessenger> {

    private UUID recruit;
    private boolean start;
    private String targetPlayer;
    private String message;

    public MessageSendMessenger() {
    }
    public MessageSendMessenger(UUID recruit, String targetPlayer, String message, boolean start) {
        this.recruit = recruit;
        this.message = message;
        this.targetPlayer = targetPlayer;
        this.start = start;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MessengerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MessengerEntity.class, context.getSender().getBoundingBox().inflate(16D));
        for (MessengerEntity messenger : list){

            if (messenger.getUUID().equals(this.recruit)){
                messenger.setTargetPlayerName(this.targetPlayer);
                messenger.setMessage(this.message);

                if(start) messenger.start();
            }

        }

    }
    public MessageSendMessenger fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.targetPlayer = buf.readUtf();
        this.message = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeUtf(targetPlayer);
        buf.writeUtf(message);
    }
}
