package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageSendMessenger implements Message<MessageSendMessenger> {

    private UUID recruit;
    private boolean start;
    private CompoundTag nbt;
    private String message;

    public MessageSendMessenger() {
    }
    public MessageSendMessenger(UUID recruit, RecruitsPlayerInfo targetPlayer, String message, boolean start) {
        this.recruit = recruit;
        this.message = message;
        this.start = start;

        if(targetPlayer != null){
            this.nbt = targetPlayer.toNBT();
        }
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MessengerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MessengerEntity.class, context.getSender().getBoundingBox().inflate(16D));
        for (MessengerEntity messenger : list){

            if (messenger.getUUID().equals(this.recruit)){
                messenger.setMessage(this.message);

                if(start){
                    messenger.start();
                }

                if(nbt != null){
                    messenger.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(this.nbt));
                }
                break;
            }

        }

    }
    public MessageSendMessenger fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.message = buf.readUtf();

        if(nbt != null){
            this.nbt = buf.readNbt();
        }
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeUtf(message);

        if(nbt != null){
            buf.writeNbt(nbt);
        }
    }
}
