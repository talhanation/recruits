package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAttackEntity implements Message<MessageAttackEntity> {

    private UUID uuid;
    private UUID target;
    private int group;

    public MessageAttackEntity(){
    }

    public MessageAttackEntity(UUID uuid, UUID target) {
        this.uuid = uuid;
        this.target = target;
        this.group = 0;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(64.0D));
        for (AbstractRecruitEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                RecruitEvents.onAttackButton(recruits,recruits.getOwner(),target, group);
                context.getSender().sendMessage(new StringTextComponent("MESSAGE"), context.getSender().getUUID());
        }

    }
    public MessageAttackEntity fromBytes(PacketBuffer buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeInt(group);
    }

}