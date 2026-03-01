package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAnswerTreaty implements Message<MessageAnswerTreaty> {

    private UUID recruit;
    private boolean accepted;

    public MessageAnswerTreaty() {
    }

    public MessageAnswerTreaty(UUID recruit, boolean accepted) {
        this.recruit = recruit;
        this.accepted = accepted;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();

        List<MessengerEntity> list = level.getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D)
        );

        for (MessengerEntity messenger : list) {
            if (messenger.getUUID().equals(this.recruit)) {
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
