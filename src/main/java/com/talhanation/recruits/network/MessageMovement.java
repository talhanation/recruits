package com.talhanation.recruits.network;

import com.talhanation.recruits.command.CommandIntent;
import com.talhanation.recruits.command.CommandIntentDispatcher;
import com.talhanation.recruits.command.CommandIntentPriority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMovement implements Message<MessageMovement> {

    private UUID player_uuid;
    private int state;
    private UUID group;
    private int formation;
    private boolean tight;
    private boolean hold;

    public MessageMovement(){
    }

    public MessageMovement(UUID player_uuid, int state, UUID group, int formation, boolean tight, boolean hold) {
        this.player_uuid = player_uuid;
        this.state  = state;
        this.group  = group;
        this.formation = formation;
        this.tight = tight;
        this.hold = hold;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        var sender = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = RecruitCommandTargetResolver.resolveGroupTargets(sender, this.player_uuid, this.group, 100D);
        CommandIntent intent = new CommandIntent.Movement(
                sender.getCommandSenderWorld().getGameTime(),
                CommandIntentPriority.NORMAL,
                false,
                this.state,
                this.formation,
                this.tight,
                this.hold,
                null
        );
        CommandIntentDispatcher.dispatch(sender, intent, list);
    }

    public MessageMovement fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readUUID();
        this.formation = buf.readInt();
        this.tight = buf.readBoolean();
        this.hold = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeInt(this.state);
        buf.writeUUID(this.group);
        buf.writeInt(this.formation);
        buf.writeBoolean(this.tight);
        buf.writeBoolean(this.hold);
    }

}
