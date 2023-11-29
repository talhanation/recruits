package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MessagePatrolLeaderSetCycle implements Message<MessagePatrolLeaderSetCycle> {

    private UUID recruit;
    private boolean cycle;

    public MessagePatrolLeaderSetCycle(){
    }

    public MessagePatrolLeaderSetCycle(UUID recruit, boolean cycle) {
        this.recruit = recruit;
        this.cycle = cycle;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractLeaderEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractLeaderEntity.class, context.getSender().getBoundingBox().inflate(100.0D));
        for (AbstractLeaderEntity recruit : list) {
            if(recruit.getUUID().equals(this.recruit))
                recruit.setCycle(this.cycle);
        }
    }

    public MessagePatrolLeaderSetCycle fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.cycle = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.cycle);
    }
}
