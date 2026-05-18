package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;
import java.util.UUID;

public class MessageAnswerTreaty implements RecruitsMessage<MessageAnswerTreaty> {

    private UUID recruit;
    private boolean accepted;

    public MessageAnswerTreaty() {
    }

    public MessageAnswerTreaty(UUID recruit, boolean accepted) {
        this.recruit = recruit;
        this.accepted = accepted;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();

        for (MessengerEntity messenger : level.getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                messenger -> messenger.getUUID().equals(this.recruit) && canAnswer(player, messenger)
        )) {

            if (accepted) {
                handleTreatyAccepted(messenger, player, level);
            }
            messenger.treatyNotAccepted = !accepted;
            messenger.teleportWaitTimer = 100;
            player.sendSystemMessage(messenger.MESSENGER_INFO_ON_MY_WAY());
            messenger.setMessengerState(MessengerEntity.MessengerState.TELEPORT_BACK);
            break;
        }
    }

    private static boolean canAnswer(ServerPlayer player, MessengerEntity messenger) {
        RecruitsFaction leaderFaction = FactionNetworkAuthority.leaderFaction(player);
        return messenger.isTreatyMessenger()
                && messenger.getTargetPlayerInfo() != null
                && player.getUUID().equals(messenger.getTargetPlayerInfo().getUUID())
                && leaderFaction != null
                && messenger.getTargetPlayerInfo().getFaction() != null
                && leaderFaction.getStringID().equals(messenger.getTargetPlayerInfo().getFaction().getStringID());
    }

    private void handleTreatyAccepted(MessengerEntity messenger, ServerPlayer player, ServerLevel level) {
        if (FactionEvents.recruitsTreatyManager == null) return;

        Team ownerTeam = messenger.getTeam();
        Team targetTeam = player.getTeam();

        if (ownerTeam == null || targetTeam == null) return;
        if (ownerTeam.getName().equals(targetTeam.getName())) return;

        int durationHours = messenger.getTreatyDurationHours();
        if (durationHours <= 0) durationHours = 1;


        FactionEvents.recruitsDiplomacyManager.setRelation(ownerTeam.getName(), targetTeam.getName(), RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL, level, false);
        FactionEvents.recruitsDiplomacyManager.setRelation(targetTeam.getName(), ownerTeam.getName(), RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL, level, false);

        FactionEvents.recruitsTreatyManager.addTreaty(ownerTeam.getName(), targetTeam.getName(), durationHours, level);

        messenger.giveDeliverItem(player);
    }

    @Override
    public MessageAnswerTreaty fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.accepted = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(accepted);
    }
}
