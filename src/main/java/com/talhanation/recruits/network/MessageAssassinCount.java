package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AssassinLeaderEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssassinCount implements Message<MessageAssassinCount> {

    private int count;
    private UUID uuid;

    public MessageAssassinCount(){
    }

    public MessageAssassinCount(int count, UUID uuid) {
        this.count = count;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AssassinLeaderEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AssassinLeaderEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AssassinLeaderEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                recruits.setCount(this.count);
        }

    }
    public MessageAssassinCount fromBytes(PacketBuffer buf) {
        this.count = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeInt(count);
        buf.writeUUID(uuid);
    }

}