package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import javax.annotation.Nullable;

public class MessagePatrolLeaderSetPatrolState implements Message<MessagePatrolLeaderSetPatrolState>, CompanionOrder<AbstractLeaderEntity> {
    private UUID recruit;
    private byte state;

    public MessagePatrolLeaderSetPatrolState() {
    }

    public MessagePatrolLeaderSetPatrolState(UUID recruit, byte state) {
        this.recruit = recruit;
        this.state = state;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.recruit;
    }

    @Override
    public Class<AbstractLeaderEntity> getCompanionType() {
        return AbstractLeaderEntity.class;
    }

    @Override
    public void applyTo(AbstractLeaderEntity leader, @Nullable ServerPlayer sender) {
        this.setState(leader);
    }

    public void setState(AbstractLeaderEntity leader) {
        AbstractLeaderEntity.State leaderState = AbstractLeaderEntity.State.fromIndex(state);
        switch (leaderState) {
            case PATROLLING -> leader.setFollowState(0);
            case STOPPED, PAUSED -> leader.setFollowState(1);
        }
        leader.setPatrolState(leaderState);
        leader.currentWaypoint = null;
    }

    public MessagePatrolLeaderSetPatrolState fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.state = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.state);
    }
}