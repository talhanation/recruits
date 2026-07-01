package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;

public class MessageTeamMainScreen implements Message<MessageTeamMainScreen> {

    public static final CustomPacketPayload.Type<MessageTeamMainScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageteammainscreen"));
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
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (!player.getUUID().equals(uuid)) {
            return;
        }
        //TeamEvents.openTeamMainScreen(player);
    }

    @Override
    public MessageTeamMainScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }


    @Override
    public CustomPacketPayload.Type<MessageTeamMainScreen> type() {
        return TYPE;
    }
}