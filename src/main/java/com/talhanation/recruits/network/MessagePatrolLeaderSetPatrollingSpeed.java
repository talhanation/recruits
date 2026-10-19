package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderSetPatrollingSpeed implements Message<MessagePatrolLeaderSetPatrollingSpeed> {

    private UUID recruit;
    private byte speed; // 0 = SLOW, 1 = NORMAL, 2 = FAST

    public MessagePatrolLeaderSetPatrollingSpeed() {}

    public MessagePatrolLeaderSetPatrollingSpeed(UUID recruit, byte speed) {
        this.recruit = recruit;
        this.speed = speed;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                context.getSender().getBoundingBox().inflate(100.0D),
                recruit -> recruit.getUUID().equals(this.recruit) && NetworkSecurity.canControl(player, recruit)
        ).forEach(leader -> leader.setPatrolSpeed(this.speed));
    }

    public MessagePatrolLeaderSetPatrollingSpeed fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.speed = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeByte(this.speed);
    }
}
