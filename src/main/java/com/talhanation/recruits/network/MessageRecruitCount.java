package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

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
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruits : list){
            //if (recruits.getUUID().equals(this.uuid))
                //CommandEvents.
        }

    }
    public MessageRecruitCount fromBytes(PacketBuffer buf) {
        this.group = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeInt(group);
        buf.writeUUID(uuid);
    }

}