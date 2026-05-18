package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.UUID;

public class MessageOpenTeamEditScreen implements RecruitsMessage<MessageOpenTeamEditScreen> {

    private UUID uuid;

    public MessageOpenTeamEditScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageOpenTeamEditScreen(Player player) {
        this.uuid = player.getUUID();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        if (!context.getSender().getUUID().equals(uuid)) {
            return;
        }
        ServerPlayer player = context.getSender();
        FactionEvents.openTeamEditScreen(player);
    }

    @Override
    public MessageOpenTeamEditScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }
}
