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

public class MessageFaceCommand implements Message<MessageFaceCommand> {

    private UUID player_uuid;
    private UUID group;
    private int formation;
    private boolean tight;
    private boolean hold;

    public MessageFaceCommand(){
    }

    public MessageFaceCommand(UUID player_uuid, UUID group, int formation, boolean tight, boolean hold) {
        this.player_uuid = player_uuid;
        this.group = group;
        this.formation = formation;
        this.tight = tight;
        this.hold = hold;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<AbstractRecruitEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, context.getSender().getBoundingBox().inflate(100));
        list.removeIf(recruit -> !recruit.isEffectedByCommand(this.player_uuid, this.group));

        CommandEvents.onFaceCommand(context.getSender(), list, this.formation, this.tight, this.hold);
    }

    public MessageFaceCommand fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.group = buf.readUUID();
        this.formation = buf.readInt();
        this.tight = buf.readBoolean();
        this.hold = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeUUID(this.group);
        buf.writeInt(this.formation);
        buf.writeBoolean(this.tight);
        buf.writeBoolean(this.hold);
    }

}
