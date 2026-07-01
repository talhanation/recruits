package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;

public class MessageOpenDisbandScreen implements Message<MessageOpenDisbandScreen> {

    public static final CustomPacketPayload.Type<MessageOpenDisbandScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageopendisbandscreen"));
    private UUID player;
    private UUID recruit;

    public MessageOpenDisbandScreen() {
        this.player = new UUID(0, 0);
    }

    public MessageOpenDisbandScreen(Player player, UUID recruit) {
        this.player = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (!player.getUUID().equals(this.player)) {
            return;
        }
        FactionEvents.openDisbandingScreen(player, recruit);
    }

    @Override
    public MessageOpenDisbandScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.recruit= buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeUUID(recruit);
    }


    @Override
    public CustomPacketPayload.Type<MessageOpenDisbandScreen> type() {
        return TYPE;
    }
}