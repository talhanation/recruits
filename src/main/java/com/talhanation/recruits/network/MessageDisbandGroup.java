package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDisbandGroup implements Message<MessageDisbandGroup> {

    private UUID owner;
    private UUID recruit;
    private boolean keepTeam;

    public MessageDisbandGroup(){
    }

    public MessageDisbandGroup(UUID owner, UUID recruit, boolean keepTeam) {
        this.owner = owner;
        this.recruit = recruit;
        this.keepTeam = keepTeam;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100D));
        int group = -1;

        for (AbstractRecruitEntity recruit1 : list){
            if(recruit1.getUUID().equals(recruit)){
                group = recruit1.getGroup();
                break;
            }
        }

        for (AbstractRecruitEntity recruit : list){
            UUID recruitOwner = recruit.getOwnerUUID();
            if (recruitOwner != null && recruitOwner.equals(owner) && recruit.getGroup() == group)
                recruit.disband(context.getSender(), keepTeam);
        }
    }

    public MessageDisbandGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.recruit = buf.readUUID();
        this.keepTeam = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeUUID(recruit);
        buf.writeBoolean(keepTeam);
    }

}