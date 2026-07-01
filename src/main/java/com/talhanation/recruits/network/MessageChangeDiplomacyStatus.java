package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
public class MessageChangeDiplomacyStatus implements Message<MessageChangeDiplomacyStatus> {
    public static final CustomPacketPayload.Type<MessageChangeDiplomacyStatus> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagechangediplomacystatus"));
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

    public void executeServerSide(IPayloadContext context){
        RecruitsDiplomacyManager.DiplomacyStatus status = RecruitsDiplomacyManager.DiplomacyStatus.fromByte(this.status);

        FactionEvents.recruitsDiplomacyManager.setRelation(ownTeam, otherTeam, status, (ServerLevel) ((ServerPlayer) context.player()).getCommandSenderWorld());

    }
    public MessageChangeDiplomacyStatus fromBytes(RegistryFriendlyByteBuf buf) {
        this.ownTeam = buf.readUtf();
        this.otherTeam = buf.readUtf();
        this.status = buf.readByte();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(ownTeam);
        buf.writeUtf(otherTeam);
        buf.writeByte(status);
    }

    @Override
    public CustomPacketPayload.Type<MessageChangeDiplomacyStatus> type() {
        return TYPE;
    }
}
