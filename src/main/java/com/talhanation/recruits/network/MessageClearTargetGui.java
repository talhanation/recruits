package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageClearTargetGui implements Message<MessageClearTargetGui>{
    private UUID recruit;

    public MessageClearTargetGui(){
    }

    public MessageClearTargetGui(UUID player) {
        this.recruit = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractRecruitEntity recruits : list) {
            if (recruits.getUUID() == this.recruit)
                RecruitEvents.onStopButton(recruits, this.recruit, 0);
        }
    }
    public MessageClearTargetGui fromBytes(PacketBuffer buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(this.recruit);
    }

}

