package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.ScoutEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageScoutTask implements RecruitsMessage<MessageScoutTask> {

    private UUID recruit;
    private int state;

    public MessageScoutTask() {
    }
    public MessageScoutTask(UUID recruit, int state) {
        this.recruit = recruit;
        this.state = state;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (this.state < ScoutEntity.State.IDLE.getIndex() || this.state > ScoutEntity.State.SEARCHING_LOST_RECRUITS.getIndex()) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D, false)
                .filter(ScoutEntity.class::isInstance)
                .map(ScoutEntity.class::cast)
                .ifPresent((scout) -> scout.startTask(ScoutEntity.State.fromIndex(state)));
    }
    public MessageScoutTask fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(state);
    }
}
