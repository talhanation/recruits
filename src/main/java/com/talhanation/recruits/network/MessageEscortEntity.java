package com.talhanation.recruits.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageEscortEntity implements Message<MessageEscortEntity> {

    private UUID uuid;
    private UUID target;
    private int group;

    public MessageEscortEntity(){
    }

    public MessageEscortEntity(UUID uuid, UUID target) {
        this.uuid = uuid;
        this.target = target;
        this.group = 0;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<Entity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(Entity.class, context.getSender().getBoundingBox().inflate(24.0D));
        for (Entity entities : list){

            if (entities.getUUID().equals(this.uuid)){

            }
                //RecruitEvents.onEscortButton(entities, target, group);
        }

    }
    public MessageEscortEntity fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeInt(group);
    }

}