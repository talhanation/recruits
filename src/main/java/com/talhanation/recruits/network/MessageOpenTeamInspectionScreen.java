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

public class MessageOpenTeamInspectionScreen implements Message<MessageOpenTeamInspectionScreen> {

    public static final CustomPacketPayload.Type<MessageOpenTeamInspectionScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageopenteaminspectionscreen"));
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
    public void executeServerSide(IPayloadContext context) {
        if (!((ServerPlayer) context.player()).getUUID().equals(uuid)) {
            return;
        }
        ServerPlayer player = ((ServerPlayer) context.player());
        //TeamEvents.openTeamInspectionScreen(player, player.getTeam());
    }

    @Override
    public MessageOpenTeamInspectionScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    @Override
    public CustomPacketPayload.Type<MessageOpenTeamInspectionScreen> type() {
        return TYPE;
    }
}
