package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageSetTargetPrio implements RecruitsMessage<MessageSetTargetPrio> {

    private UUID recruit;
    private int state;

    public MessageSetTargetPrio() {
    }
    public MessageSetTargetPrio(UUID recruit, int state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D)
                .filter(IHasTargetPriority.class::isInstance)
                .map(IHasTargetPriority.class::cast)
                .ifPresent(specialRecruit -> specialRecruit.setTargetPriority(IHasTargetPriority.TargetPriority.fromIndex(state)));
    }
    public MessageSetTargetPrio fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }
}
