package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.diplomacy.DiplomacyTeamList;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;


public class MessageToClientUpdateDiplomacyList implements Message<MessageToClientUpdateDiplomacyList> {
    private CompoundTag teamsNbt;
    private CompoundTag diplomacyNbt;
    public MessageToClientUpdateDiplomacyList() {
    }

    public MessageToClientUpdateDiplomacyList(List<RecruitsTeam> teamList, Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyStatusMap) {
        this.teamsNbt = RecruitsTeam.toNBT(teamList);
        this.diplomacyNbt = RecruitsDiplomacyManager.mapToNbt(diplomacyStatusMap);
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        DiplomacyTeamList.teams = RecruitsTeam.getListFromNBT(teamsNbt);
        DiplomacyTeamList.diplomacyMap = RecruitsDiplomacyManager.mapFromNbt(diplomacyNbt);
    }

    @Override
    public MessageToClientUpdateDiplomacyList fromBytes(FriendlyByteBuf buf) {
        this.teamsNbt = buf.readNbt();
        this.diplomacyNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.teamsNbt);
        buf.writeNbt(this.diplomacyNbt);
    }

}