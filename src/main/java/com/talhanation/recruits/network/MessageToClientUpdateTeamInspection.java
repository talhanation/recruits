package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.team.TeamInspectionScreen;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateTeamInspection implements Message<MessageToClientUpdateTeamInspection> {
    private CompoundTag players;
    private CompoundTag team;
    public MessageToClientUpdateTeamInspection() {
    }

    public MessageToClientUpdateTeamInspection(List<RecruitsPlayerInfo> playerInfoList, RecruitsTeam team) {
        this.players = RecruitsPlayerInfo.toNBT(playerInfoList);
        this.team = team.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        PlayersList.onlinePlayers = RecruitsPlayerInfo.getListFromNBT(players);
        TeamInspectionScreen.recruitsTeam = RecruitsTeam.fromNBT(team);
    }

    @Override
    public MessageToClientUpdateTeamInspection fromBytes(FriendlyByteBuf buf) {
        this.players = buf.readNbt();
        this.team = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.players);
        buf.writeNbt(this.team);
    }

}