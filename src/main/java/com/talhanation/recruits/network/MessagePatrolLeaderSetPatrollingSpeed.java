package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MessagePatrolLeaderSetPatrollingSpeed implements Message<MessagePatrolLeaderSetPatrollingSpeed> {

    private UUID recruit;
    private boolean fast;

    public MessagePatrolLeaderSetPatrollingSpeed(){
    }

    public MessagePatrolLeaderSetPatrollingSpeed(UUID recruit, boolean fast) {
        this.recruit = recruit;
        this.fast = fast;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractLeaderEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractLeaderEntity.class, context.getSender().getBoundingBox().inflate(100.0D));
        for (AbstractLeaderEntity recruit : list) {
            if(recruit.getUUID().equals(this.recruit))
                recruit.setFastPatrolling(this.fast);
        }
    }

    public MessagePatrolLeaderSetPatrollingSpeed fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.fast = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.fast);
    }
}
