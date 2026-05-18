package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageRecruitGui implements RecruitsMessage<MessageRecruitGui> {

    private UUID uuid;
    private UUID recruit;


    public MessageRecruitGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageRecruitGui(Player player, UUID recruit) {
        this.uuid = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!player.getUUID().equals(uuid)) {
            return;
        }

        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16.0D, false)
                .ifPresent(recruit -> recruit.openGUI(player));
    }

    @Override
    public MessageRecruitGui fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(recruit);
    }
}
