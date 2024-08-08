package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageFormationFollowMovement implements Message<MessageFormationFollowMovement> {

    private UUID player_uuid;

    private int group;
    private int formation;

    public MessageFormationFollowMovement(){
    }

    public MessageFormationFollowMovement(UUID player_uuid, int group, int formation) {
        this.player_uuid = player_uuid;
        this.group  = group;
        this.formation = formation;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100));
        list.removeIf(recruit -> !recruit.isEffectedByCommand(this.player_uuid, this.group));

        CommandEvents.applyFormation(formation, list, context.getSender(), context.getSender().position());
    }

    public MessageFormationFollowMovement fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.group = buf.readInt();
        this.formation = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeInt(this.group);
        buf.writeInt(this.formation);
    }

}