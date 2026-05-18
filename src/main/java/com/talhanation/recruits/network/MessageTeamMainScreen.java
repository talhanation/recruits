package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.UUID;

public class MessageTeamMainScreen implements RecruitsMessage<MessageTeamMainScreen> {

    private UUID uuid;

    public MessageTeamMainScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageTeamMainScreen(Player player) {
        this.uuid = player.getUUID();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (!player.getUUID().equals(uuid)) {
            return;
        }
        //TeamEvents.openTeamMainScreen(player);
    }

    @Override
    public MessageTeamMainScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

}