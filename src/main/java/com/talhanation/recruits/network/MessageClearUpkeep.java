package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageClearUpkeep implements RecruitsMessage<MessageClearUpkeep> {
    private UUID uuid;
    private UUID group;

    public MessageClearUpkeep() {
    }

    public MessageClearUpkeep(UUID uuid, UUID group) {
        this.uuid = uuid;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitCommandTargetResolver.resolveGroupTargets(player, this.uuid, this.group, 100D)
                .forEach((recruit) -> CommandEvents.onClearUpkeepButton(player.getUUID(), recruit, group));
    }

    public MessageClearUpkeep fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(group);
    }
}
