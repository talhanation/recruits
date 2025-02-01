package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageStrategicFire implements Message<MessageStrategicFire> {

    private UUID player;
    private int group;
    private boolean should;

    public MessageStrategicFire() {
    }

    public MessageStrategicFire(UUID player, int group, boolean should) {
        this.player = player;
        this.group = group;
        this.should = should;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        serverPlayer.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(100)
        ).forEach((recruit) ->
                CommandEvents.onStrategicFireCommand(
                        serverPlayer,
                        this.player,
                        recruit,
                        group,
                        should
                )
        );
    }

    public MessageStrategicFire fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readInt();
        this.should = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.group);
        buf.writeBoolean(this.should);
    }
}