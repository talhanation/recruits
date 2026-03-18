package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public class MessageRemoveFromTeam implements Message<MessageRemoveFromTeam> {

    private String player;

    public MessageRemoveFromTeam() {
    }

    public MessageRemoveFromTeam(String player) {
        this.player = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer sender = Objects.requireNonNull(context.getSender());
        ServerLevel level = sender.serverLevel();

        boolean foundOnline = false;
        for (ServerPlayer serverPlayer : level.players()) {
            if (serverPlayer.getName().getString().equals(player)) {
                FactionEvents.tryToRemoveFromTeam(
                        serverPlayer.getTeam(),
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
