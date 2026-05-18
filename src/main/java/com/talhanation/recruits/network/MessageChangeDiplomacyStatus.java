package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;


public class MessageChangeDiplomacyStatus implements RecruitsMessage<MessageChangeDiplomacyStatus> {
    private String ownTeam;
    private String otherTeam;
    private byte status;

    public MessageChangeDiplomacyStatus(){
    }

    public MessageChangeDiplomacyStatus(RecruitsFaction ownTeam, RecruitsFaction otherTeam, RecruitsDiplomacyManager.DiplomacyStatus status) {
        this.status = status.getByteValue();
        this.ownTeam = ownTeam.getStringID();
        this.otherTeam = otherTeam.getStringID();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (!FactionNetworkAuthority.isLeaderOf(player, ownTeam)) {
            return;
        }
        if (ownTeam.equals(otherTeam)) {
            return;
        }
        if (FactionEvents.recruitsFactionManager.getFactionByStringID(otherTeam) == null) {
            return;
        }
        if (this.status < RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL.getByteValue()
                || this.status > RecruitsDiplomacyManager.DiplomacyStatus.ENEMY.getByteValue()) {
            return;
        }
        RecruitsDiplomacyManager.DiplomacyStatus status = RecruitsDiplomacyManager.DiplomacyStatus.fromByte(this.status);

        FactionEvents.recruitsDiplomacyManager.setRelation(ownTeam, otherTeam, status, (ServerLevel) player.getCommandSenderWorld());

    }
    public MessageChangeDiplomacyStatus fromBytes(FriendlyByteBuf buf) {
        this.ownTeam = buf.readUtf();
        this.otherTeam = buf.readUtf();
        this.status = buf.readByte();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(ownTeam);
        buf.writeUtf(otherTeam);
        buf.writeByte(status);
    }
}
