package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAggro implements Message<MessageAggro> {

    private UUID player;
    private UUID recruit;
    private int state;
    private int group;
    private boolean fromGui;


    public MessageAggro(){
    }

    public MessageAggro(UUID player, int state, int group) {
        this.player = player;
        this.state  = state;
        this.group  = group;
        this.fromGui = false;
        this.recruit = null;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        if (fromGui){
            List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
            for (AbstractRecruitEntity recruits : list) {
                if (recruits.getUUID().equals(this.recruit))
                CommandEvents.onXKeyPressed(this.player, recruits, this.state, group, fromGui);
            }
        }
        else {
            List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(64.0D));
            for (AbstractRecruitEntity recruits : list) {
                CommandEvents.onXKeyPressed(this.player, recruits, this.state, group, fromGui);
            }
        }
    }

    public MessageAggro fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readInt();
        if (this.recruit != null) this.recruit = buf.readUUID();
        this.fromGui = buf.readBoolean();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeInt(this.group);
        buf.writeBoolean(this.fromGui);
        if (this.recruit != null) buf.writeUUID(this.recruit);
    }

}