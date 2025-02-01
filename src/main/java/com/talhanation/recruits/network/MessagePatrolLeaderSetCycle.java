package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;


public class MessagePatrolLeaderSetCycle implements Message<MessagePatrolLeaderSetCycle> {

    private UUID recruit;
    private boolean cycle;

    public MessagePatrolLeaderSetCycle() {
    }

    public MessagePatrolLeaderSetCycle(UUID recruit, boolean cycle) {
        this.recruit = recruit;
        this.cycle = cycle;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                (leader) -> leader.getUUID().equals(this.recruit)
        ).forEach(leader -> leader.setCycle(this.cycle));
    }

    public MessagePatrolLeaderSetCycle fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.cycle = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.cycle);
    }
}
