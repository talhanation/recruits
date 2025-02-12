package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
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

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                (messenger) -> messenger.getUUID().equals(this.recruit)
        ).forEach((messenger) -> {
            if (messenger.getUUID().equals(this.recruit)){

                messenger.setMessage(this.message);

                if(!this.nbt.isEmpty()){
                    messenger.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(this.nbt));
                }

                if(start){
                    messenger.start();
                }
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
