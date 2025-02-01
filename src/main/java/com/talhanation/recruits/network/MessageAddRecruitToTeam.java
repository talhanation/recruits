package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public class MessageAddRecruitToTeam implements Message<MessageAddRecruitToTeam> {

    private String teamName;
    private int x;

    public MessageAddRecruitToTeam(){
    }

    public MessageAddRecruitToTeam(String teamName, int x) {
        this.teamName = teamName;
        this.x = x;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerLevel level = Objects.requireNonNull(context.getSender()).serverLevel();

        TeamEvents.addNPCToData(level, teamName, x);
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
