package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;

public class MessageRemoveFromTeam implements Message<MessageRemoveFromTeam> {

    public static final CustomPacketPayload.Type<MessageRemoveFromTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageremovefromteam"));
    private String player;

    public MessageRemoveFromTeam() {
    }

    public MessageRemoveFromTeam(String player) {
        this.player = player;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer sender = Objects.requireNonNull(((ServerPlayer) context.player()));
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

    public MessageRemoveFromTeam fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(player);
    }

    @Override
    public CustomPacketPayload.Type<MessageRemoveFromTeam> type() {
        return TYPE;
    }
}
