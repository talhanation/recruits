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

    public MessageFollow(){
    }

    public MessageFollow(UUID player) {
        this.player = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = context.getSender().level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(40.0D));
        for (AbstractRecruitEntity recruits : list){
                KeyEvents.onRKeyPressed(this.player, recruits);
        }
    }
    public MessageFollow fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
    }

}