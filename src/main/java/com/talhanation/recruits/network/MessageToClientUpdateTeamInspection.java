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
    private boolean editing;
    private boolean managing;

    public MessageToClientUpdateTeamInspection() {
    }

    public MessageToClientUpdateTeamInspection(List<RecruitsPlayerInfo> playerInfoList, RecruitsTeam team, boolean editing, boolean managing) {
        this.players = RecruitsPlayerInfo.toNBT(playerInfoList);
        this.team = team.toNBT();
        this.editing = editing;
        this.managing = managing;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        PlayersList.onlinePlayers = RecruitsPlayerInfo.getListFromNBT(players);
        TeamInspectionScreen.recruitsTeam = RecruitsTeam.fromNBT(team);
        TeamInspectionScreen.isEditingAllowed = editing;
        TeamInspectionScreen.isManagingAllowed = managing;
    }

    @Override
    public MessageToClientUpdateTeamInspection fromBytes(FriendlyByteBuf buf) {
        this.players = buf.readNbt();
        this.team = buf.readNbt();
        this.editing = buf.readBoolean();
        this.managing = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.players);
        buf.writeNbt(this.team);
        buf.writeBoolean(editing);
        buf.writeBoolean(managing);
    }

}