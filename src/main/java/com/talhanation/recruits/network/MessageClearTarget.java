package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageClearTarget implements Message<MessageClearTarget>{
    private UUID player;
    private int group;

    public MessageClearTarget(){
    }

    public MessageClearTarget(UUID player, int group) {
        this.player = player;
        this.group = group;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(64.0D));
        for (AbstractRecruitEntity recruits : list) {
            RecruitEvents.onStopButton(recruits, this.player, group);
        }
    }
    public MessageClearTarget fromBytes(PacketBuffer buf) {
        this.player = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.group);
    }

}

