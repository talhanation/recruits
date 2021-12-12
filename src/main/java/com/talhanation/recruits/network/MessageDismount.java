package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDismount implements Message<MessageDismount> {

    private UUID uuid;
    private int group;

    public MessageDismount(){
    }

    public MessageDismount(UUID uuid, int group) {
        this.uuid = uuid;
        this.group = group;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(64.0D));
        for (AbstractRecruitEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid) && recruits.getVehicle() != null && (group == recruits.getGroup() || group == 0))
                recruits.stopRiding();
        }

    }
    public MessageDismount fromBytes(PacketBuffer buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(uuid);
        buf.writeInt(group);
    }

}