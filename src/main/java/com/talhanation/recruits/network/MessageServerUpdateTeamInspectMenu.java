package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageServerUpdateTeamInspectMenu implements Message<MessageServerUpdateTeamInspectMenu> {

    public String teamName;

    public MessageServerUpdateTeamInspectMenu() {
    }

    public MessageServerUpdateTeamInspectMenu(Team team) {
        this.teamName = team.getName();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        //Main.LOGGER.debug("---------MessageServerUpdateTeamInspectMenu--------");
        //Main.LOGGER.debug("teamName: " + teamName);
        TeamEvents.updateTeamInspectMenu(context.getSender(), context.getSender().serverLevel(), teamName);

    }

    @Override
    public MessageServerUpdateTeamInspectMenu fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(teamName);
    }

}