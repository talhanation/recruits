package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;

public class MessageRemoveFromTeam implements RecruitsMessage<MessageRemoveFromTeam> {

    private String player;

    public MessageRemoveFromTeam() {
    }

    public MessageRemoveFromTeam(String player) {
        this.player = player;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer sender = Objects.requireNonNull(context.getSender());
        ServerLevel level = sender.serverLevel();
        RecruitsFaction senderFaction = FactionNetworkAuthority.leaderFaction(sender);
        if (senderFaction == null) {
            return;
        }

        boolean foundOnline = false;
        for (ServerPlayer serverPlayer : level.players()) {
            if (serverPlayer.getName().getString().equals(player)
                    && serverPlayer.getTeam() != null
                    && serverPlayer.getTeam().getName().equals(senderFaction.getStringID())) {
                FactionEvents.tryToRemoveFromTeam(
                        sender.getTeam(),
                        sender,
                        serverPlayer,
                        level,
                        player,
                        true
                );
                foundOnline = true;
                break;
            }
        }

        if (!foundOnline) {
            FactionEvents.removeOfflinePlayerFromTeam(sender, player, level);
        }
    }

    public MessageRemoveFromTeam fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(player);
    }
}
