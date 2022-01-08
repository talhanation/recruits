package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDisband implements Message<MessageDisband> {

    private UUID uuid;

    public MessageDisband(){
    }

    public MessageDisband(UUID uuid) {
        this.uuid = uuid;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(8D));
        for (AbstractRecruitEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                recruits.disband(context.getSender());
        }

    }
    public MessageDisband fromBytes(PacketBuffer buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(uuid);
    }

}