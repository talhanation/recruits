package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageRecruitCount implements Message<MessageRecruitCount> {

    private int group;
    private UUID uuid;

    public MessageRecruitCount(){
    }

    public MessageRecruitCount(int group, UUID uuid) {
        this.group = group;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruits : list){
            //if (recruits.getUUID().equals(this.uuid))
                //CommandEvents.
        }

    }
    public MessageRecruitCount fromBytes(FriendlyByteBuf buf) {
        this.group = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(group);
        buf.writeUUID(uuid);
    }

}