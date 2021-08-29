package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageFollow implements Message<MessageFollow> {

    private UUID player;
    private int state;
    private int group;

    public MessageFollow(){
    }

    public MessageFollow(UUID player, int state, int group) {
        this.player = player;
        this.state  = state;
        this.group  = group;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = context.getSender().level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(40.0D));
        for (AbstractRecruitEntity recruits : list){
                KeyEvents.onRKeyPressed(this.player, recruits, this.state, this.group);
        }
    }
    public MessageFollow fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeInt(this.group);
    }

}