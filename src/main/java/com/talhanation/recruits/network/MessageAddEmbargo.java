package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageAddEmbargo implements Message<MessageAddEmbargo> {

    public static final CustomPacketPayload.Type<MessageAddEmbargo> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageaddembargo"));
    private UUID embargoPlayer;

    public MessageAddEmbargo() {
    }

    public MessageAddEmbargo(UUID embargoPlayer) {
        this.embargoPlayer = embargoPlayer;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        ServerLevel level = (ServerLevel) player.level();

        // Guard: only leaders may add embargoes
        RecruitsFaction ownFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(
                player.getTeam() != null ? player.getTeam().getName() : ""
        );
        if (ownFaction == null || !ownFaction.getTeamLeaderUUID().equals(player.getUUID())) return;

        FactionEvents.recruitsDiplomacyManager.addEmbargo(embargoPlayer, ownFaction.getStringID(), level);
    }

    @Override
    public MessageAddEmbargo fromBytes(RegistryFriendlyByteBuf buf) {
        this.embargoPlayer = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(embargoPlayer);
    }

    @Override
    public CustomPacketPayload.Type<MessageAddEmbargo> type() {
        return TYPE;
    }
}
