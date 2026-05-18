package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.UUID;

public class MessageOpenTeamInspectionScreen implements RecruitsMessage<MessageOpenTeamInspectionScreen> {

    private UUID uuid;

    public MessageOpenTeamInspectionScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageOpenTeamInspectionScreen(Player player) {
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
        //TeamEvents.openTeamInspectionScreen(player, player.getTeam());
    }

    @Override
    public MessageOpenTeamInspectionScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }
}
