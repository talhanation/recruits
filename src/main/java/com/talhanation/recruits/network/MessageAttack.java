package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageAttack implements Message<MessageAttack> {

    private UUID player;
    private int state;

    public MessageAttack(){
    }

    public MessageAttack(UUID player, int state) {
        this.player = player;
        this.state  = state;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = context.getSender().level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(40.0D));
        for (AbstractRecruitEntity recruits : list){
            KeyEvents.onXKeyPressed(this.player, recruits, this.state);
        }
    }
    public MessageAttack fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
    }

}