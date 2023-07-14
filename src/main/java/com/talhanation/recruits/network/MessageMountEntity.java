package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import org.checkerframework.checker.units.qual.A;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMountEntity implements Message<MessageMountEntity> {

    private UUID uuid;
    private UUID target;
    private int group;

    public MessageMountEntity(){
    }

    public MessageMountEntity(UUID uuid, UUID target, int group) {
        this.uuid = uuid;
        this.target = target;
        this.group = group;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<Entity> entityList = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(Entity.class, context.getSender().getBoundingBox().inflate(100));
        for(Entity mount : entityList){
            if(mount.getUUID().equals(target) && RecruitsModConfig.MountWhiteList.get().contains(mount.getEncodeId())){

                List<AbstractRecruitEntity> recruitList = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100));
                for (AbstractRecruitEntity recruits : recruitList) {
                    CommandEvents.onMountButton(uuid, recruits, target, group);
                }
            }
        }
    }
    public MessageMountEntity fromBytes(FriendlyByteBuf buf) {
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