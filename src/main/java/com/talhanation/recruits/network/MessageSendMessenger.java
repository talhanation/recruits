package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageSendMessenger implements RecruitsMessage<MessageSendMessenger> {

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
        else {
            this.nbt = new CompoundTag();
        }

    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D, false)
                .filter(messenger -> messenger instanceof MessengerEntity)
                .map(messenger -> (MessengerEntity) messenger)
                .ifPresent((messenger) -> {
                    messenger.setMessage(this.message);

                    if(!this.nbt.isEmpty()){
                        messenger.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(this.nbt));
                    }

                    if(start){
                        messenger.setIsTreatyMessenger(false);
                        messenger.start();
                    }
                });
    }

    public MessageSendMessenger fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.message = buf.readUtf();
        this.nbt = buf.readNbt();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeUtf(message);
        buf.writeNbt(nbt);
    }
}
