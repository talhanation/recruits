package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageClearTargetGui implements RecruitsMessage<MessageClearTargetGui> {
    private UUID recruit;
    private UUID player;

    public MessageClearTargetGui() {
    }

    public MessageClearTargetGui(UUID player, UUID recruit) {
        this.player = player;
        this.recruit = recruit;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!player.getUUID().equals(this.player)) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16.0D)
                .ifPresent((recruit) -> CommandEvents.onClearTargetButton(player.getUUID(), recruit, null));
    }

    public MessageClearTargetGui fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.recruit);
    }
}
