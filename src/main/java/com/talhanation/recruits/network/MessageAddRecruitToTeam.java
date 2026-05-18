package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;

public class MessageAddRecruitToTeam implements RecruitsMessage<MessageAddRecruitToTeam> {

    private String teamName;
    private int x;

    public MessageAddRecruitToTeam(){
    }

    public MessageAddRecruitToTeam(String teamName, int x) {
        this.teamName = teamName;
        this.x = x;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerLevel level = Objects.requireNonNull(context.getSender()).serverLevel();

        FactionEvents.addNPCToData(level, teamName, x);
    }

    public MessageAddRecruitToTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.x = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeInt(this.x);
    }
}
