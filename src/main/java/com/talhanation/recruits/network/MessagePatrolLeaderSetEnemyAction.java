package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetEnemyAction implements Message<MessagePatrolLeaderSetEnemyAction> {

    private UUID recruit;
    private byte action; // 0 = CHARGE, 1 = HOLD, 2 = KEEP_PATROLLING

    public MessagePatrolLeaderSetEnemyAction() {}

    public MessagePatrolLeaderSetEnemyAction(UUID recruit, byte action) {
        this.recruit = recruit;
        this.action = action;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                leader -> leader.getUUID().equals(this.recruit) && leader.isAlive()
        ).forEach(leader -> leader.setEnemyAction(this.action));
    }

    public MessagePatrolLeaderSetEnemyAction fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.action = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.action);
    }
}
