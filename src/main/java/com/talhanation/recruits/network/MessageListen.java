package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageListen implements Message<MessageListen> {

    private boolean bool;
    private UUID uuid;

    public MessageListen(){
    }

    public MessageListen(boolean bool, UUID uuid) {
        this.bool = bool;
        this.uuid = uuid;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100));
        for (AbstractRecruitEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                recruits.setListen(bool);
        }

    }
    public MessageListen fromBytes(FriendlyByteBuf buf) {
        this.bool = buf.readBoolean();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(bool);
        buf.writeUUID(uuid);
    }

}