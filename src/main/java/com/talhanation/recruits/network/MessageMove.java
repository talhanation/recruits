package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.KeyEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageMove implements Message<MessageMove> {

    private UUID player;

    public MessageMove(){
    }

    public MessageMove(UUID player) {
        this.player = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = context.getSender().level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(40.0D));
        for (AbstractRecruitEntity recruits : list) {
                KeyEvents.onCKeyPressed(this.player, recruits);
        }
    }
    public MessageMove fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
    }

}