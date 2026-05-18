package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageOpenPromoteScreen implements RecruitsMessage<MessageOpenPromoteScreen> {

    private UUID player;
    private UUID recruit;

    public MessageOpenPromoteScreen() {
        this.player = new UUID(0, 0);
    }

    public MessageOpenPromoteScreen(Player player, UUID recruit) {
        this.player = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!player.getUUID().equals(this.player)) {
            return;
        }

        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16.0D, false)
                .filter(MessagePromoteRecruit::canPromote)
                .ifPresent((recruit) -> RecruitEvents.openPromoteScreen(player, recruit));
    }

    @Override
    public MessageOpenPromoteScreen fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeUUID(recruit);
    }
}
