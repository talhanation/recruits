package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageClearTarget implements RecruitsMessage<MessageClearTarget> {
    private UUID uuid;
    private UUID group;

    public MessageClearTarget(){
    }

    public MessageClearTarget(UUID uuid, UUID group) {
        this.uuid = uuid;
        this.group = group;

    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = RecruitCommandTargetResolver.resolveGroupTargets(player, this.uuid, this.group, 100D);
        for (AbstractRecruitEntity recruits : list) {
            CommandEvents.onClearTargetButton(player.getUUID(), recruits, group);
        }
    }
    public MessageClearTarget fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(group);
    }

}
