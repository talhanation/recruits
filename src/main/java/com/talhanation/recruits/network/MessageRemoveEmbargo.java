package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageRemoveEmbargo implements Message<MessageRemoveEmbargo> {

    private UUID embargoedPlayerUUID;

    public MessageRemoveEmbargo() {
    }

    public MessageRemoveEmbargo(UUID embargoedPlayerUUID) {
        this.embargoedPlayerUUID = embargoedPlayerUUID;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel level = (ServerLevel) player.level();

        RecruitsFaction ownFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(
                player.getTeam() != null ? player.getTeam().getName() : ""
        );
        if (ownFaction == null || !ownFaction.getTeamLeaderUUID().equals(player.getUUID())) return;

        FactionEvents.recruitsDiplomacyManager.removeEmbargo(embargoedPlayerUUID, ownFaction.getStringID(), level);
    }

    @Override
    public MessageRemoveEmbargo fromBytes(FriendlyByteBuf buf) {
        this.embargoedPlayerUUID = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(embargoedPlayerUUID);
    }
}
