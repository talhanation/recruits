package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageDiplomacyChangeStatus implements Message<MessageDiplomacyChangeStatus> {
    private String ownTeam;
    private String otherTeam;
    private byte status;

    public MessageDiplomacyChangeStatus(){
    }

    public MessageDiplomacyChangeStatus(RecruitsTeam ownTeam, RecruitsTeam otherTeam, RecruitsDiplomacyManager.DiplomacyStatus status) {
        this.status = status.getByteValue();
        this.ownTeam = ownTeam.getStringID();
        this.otherTeam = otherTeam.getStringID();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        RecruitsDiplomacyManager.DiplomacyStatus status = RecruitsDiplomacyManager.DiplomacyStatus.fromByte(this.status);

        TeamEvents.recruitsDiplomacyManager.setRelation(ownTeam, otherTeam, status, (ServerLevel) context.getSender().getCommandSenderWorld());

    }
    public MessageDiplomacyChangeStatus fromBytes(FriendlyByteBuf buf) {
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
