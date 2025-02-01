package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.team.RecruitsTeamList;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateTeamList implements Message<MessageToClientUpdateTeamList> {
    private CompoundTag nbt;

    public MessageToClientUpdateTeamList() {
    }

    public MessageToClientUpdateTeamList(List<RecruitsTeam> teamList) {
        this.nbt = RecruitsTeam.toNBT(teamList);
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        RecruitsTeamList.teams = RecruitsTeam.getListFromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateTeamList fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

}