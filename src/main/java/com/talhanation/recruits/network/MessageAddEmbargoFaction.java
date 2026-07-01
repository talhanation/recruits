package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;

public class MessageAddEmbargoFaction implements Message<MessageAddEmbargoFaction> {

    public static final CustomPacketPayload.Type<MessageAddEmbargoFaction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageaddembargofaction"));
    private String faction;

    public MessageAddEmbargoFaction() {
    }

    public MessageAddEmbargoFaction(String faction) {
        this.faction = faction;
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

        RecruitsFaction embargoFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(faction);

        if (ownFaction == null || embargoFaction == null || !ownFaction.getTeamLeaderUUID().equals(player.getUUID())) return;

        for(RecruitsPlayerInfo info : embargoFaction.getMembers()){
            FactionEvents.recruitsDiplomacyManager.addEmbargo(info.getUUID(), ownFaction.getStringID(), level);
        }


    }

    @Override
    public MessageAddEmbargoFaction fromBytes(RegistryFriendlyByteBuf buf) {
        this.faction = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(faction);
    }

    @Override
    public CustomPacketPayload.Type<MessageAddEmbargoFaction> type() {
        return TYPE;
    }
}
