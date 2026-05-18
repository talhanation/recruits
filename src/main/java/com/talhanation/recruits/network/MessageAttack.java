package com.talhanation.recruits.network;

import com.talhanation.recruits.command.CommandIntent;
import com.talhanation.recruits.command.CommandIntentDispatcher;
import com.talhanation.recruits.command.CommandIntentPriority;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAttack implements RecruitsMessage<MessageAttack> {

    private UUID playerUuid;
    private UUID group;

    public MessageAttack() {
    }

    public MessageAttack(UUID playerUuid, UUID group) {
        this.playerUuid = playerUuid;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = RecruitCommandTargetResolver.resolveGroupTargets(serverPlayer, playerUuid, group, 100D);
        CommandIntent intent = new CommandIntent.Attack(
                serverPlayer.getCommandSenderWorld().getGameTime(),
                CommandIntentPriority.NORMAL,
                false,
                group
        );
        CommandIntentDispatcher.dispatch(serverPlayer, intent, list);
    }

    public MessageAttack fromBytes(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerUuid);
        buf.writeUUID(this.group);
    }
}
