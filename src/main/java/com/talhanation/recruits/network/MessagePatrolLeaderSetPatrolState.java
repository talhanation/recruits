package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessagePatrolLeaderSetPatrolState implements Message<MessagePatrolLeaderSetPatrolState> {
    private UUID recruit;
    private byte state;

    public MessagePatrolLeaderSetPatrolState() {}

    public MessagePatrolLeaderSetPatrolState(UUID recruit, byte state) {
        this.recruit = recruit;
        this.state = state;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(AbstractLeaderEntity.class, player.getBoundingBox()
                .inflate(16.0D), v -> v
                .getUUID()
                .equals(this.recruit))
                .stream()
                .filter(AbstractLeaderEntity::isAlive)
                .findAny()
                .ifPresent(abstractRecruitEntity -> abstractRecruitEntity.setPatrollingState(state, true));

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