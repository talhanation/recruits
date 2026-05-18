package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageDismountGui implements RecruitsMessage<MessageDismountGui> {

    private UUID uuid;
    private UUID player;

    public MessageDismountGui() {
    }

    public MessageDismountGui(UUID player, UUID uuid) {
        this.player = player;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        if (!serverPlayer.getUUID().equals(this.player)) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedRecruit(serverPlayer, this.uuid, 16.0D)
                .ifPresent((recruit) -> CommandEvents.onDismountButton(serverPlayer.getUUID(), recruit, null));
    }

    public MessageDismountGui fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.player = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(player);
    }
}
