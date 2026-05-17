package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageSendTreaty implements Message<MessageSendTreaty> {

    private UUID recruit;
    private boolean start;
    private CompoundTag targetPlayerNbt;
    private int durationHours;

    public MessageSendTreaty() {
    }

    public MessageSendTreaty(UUID recruit, RecruitsPlayerInfo targetPlayer, int durationHours, boolean start) {
        this.recruit = recruit;
        this.durationHours = durationHours;
        this.start = start;

        if (targetPlayer != null) {
            this.targetPlayerNbt = targetPlayer.toNBT();
        } else {
            this.targetPlayerNbt = new CompoundTag();
        }
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (this.durationHours < 0) {
            return;
        }
        RecruitsPlayerInfo targetPlayer = RecruitsPlayerInfo.getFromNBT(this.targetPlayerNbt);
        if (!isFactionLeader(targetPlayer)) {
            return;
        }
        RecruitsFaction senderFaction = FactionNetworkAuthority.leaderFaction(player);
        if (senderFaction == null) {
            return;
        }
        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 16D, false)
                .filter(messenger -> messenger instanceof MessengerEntity)
                .map(messenger -> (MessengerEntity) messenger)
                .filter(messenger -> messenger.getTeam() != null && messenger.getTeam().getName().equals(senderFaction.getStringID()))
                .ifPresent((messenger) -> {
                    if (!this.targetPlayerNbt.isEmpty()) {
                        messenger.setTargetPlayerInfo(targetPlayer);
                    }

                    if (start) {
                        messenger.setTreatyDurationHours(this.durationHours);
                        messenger.setIsTreatyMessenger(true);
                        messenger.start();
                    }
                });
    }

    private static boolean isFactionLeader(RecruitsPlayerInfo playerInfo) {
        if (playerInfo == null || playerInfo.getFaction() == null) {
            return false;
        }
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(playerInfo.getFaction().getStringID());
        return faction != null && playerInfo.getUUID().equals(faction.getTeamLeaderUUID());
    }

    @Override
    public MessageSendTreaty fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.durationHours = buf.readInt();
        this.targetPlayerNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeInt(durationHours);
        buf.writeNbt(targetPlayerNbt);
    }
}
