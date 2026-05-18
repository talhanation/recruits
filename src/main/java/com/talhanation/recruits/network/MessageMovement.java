package com.talhanation.recruits.network;

import com.talhanation.recruits.command.CommandIntent;
import com.talhanation.recruits.command.CommandIntentDispatcher;
import com.talhanation.recruits.command.CommandIntentPriority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMovement implements RecruitsMessage<MessageMovement> {

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

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
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
