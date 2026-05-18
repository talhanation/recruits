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

public class MessageRangedFire implements RecruitsMessage<MessageRangedFire> {

    private UUID player;
    private UUID group;
    private boolean should;

    public MessageRangedFire(){
    }

    public MessageRangedFire(UUID player, UUID group, boolean shields) {
        this.player = player;
        this.group = group;
        this.should = shields;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = RecruitCommandTargetResolver.resolveGroupTargets(serverPlayer, this.player, this.group, 100D);
        for (AbstractRecruitEntity recruits : list) {
            CommandEvents.onRangedFireCommand(serverPlayer, serverPlayer.getUUID(), recruits, group, should);
        }
    }
    public MessageRangedFire fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.should = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.should);
    }

}
