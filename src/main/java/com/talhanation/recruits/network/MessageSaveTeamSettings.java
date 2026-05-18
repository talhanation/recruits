package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;


public class MessageSaveTeamSettings implements RecruitsMessage<MessageSaveTeamSettings> {
    private CompoundTag nbt;
    private String stringID;
    private int cost;

    public MessageSaveTeamSettings() {
    }

    public MessageSaveTeamSettings(RecruitsFaction team, int cost) {
        this.nbt = team.toNBT();
        this.stringID = team.getStringID();
        this.cost = cost;
    }

    @Override
    public PacketFlow getExecutingSide()  {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (player == null || !FactionNetworkAuthority.isLeaderOf(player, stringID)) {
            return;
        }
        RecruitsFaction editedTeam = RecruitsFaction.fromNBT(nbt);
        RecruitsFaction currentTeam = FactionEvents.recruitsFactionManager.getFactionByStringID(stringID);
        if (editedTeam == null || currentTeam == null || !stringID.equals(editedTeam.getStringID())) {
            return;
        }
        if (!hasValidSettings(editedTeam)) {
            return;
        }
        RecruitsPlayerInfo newLeader = FactionNetworkAuthority.memberByUuid(currentTeam, new RecruitsPlayerInfo(editedTeam.getTeamLeaderUUID(), editedTeam.getTeamLeaderName()));
        if (newLeader == null) {
            return;
        }
        editedTeam.setTeamLeaderName(newLeader.getName());
        FactionEvents.modifyTeam(player.server.overworld(), stringID, editedTeam, player, getEditCost(currentTeam, editedTeam));
    }

    private static boolean hasValidSettings(RecruitsFaction editedTeam) {
        return editedTeam.getMaxNPCsPerPlayer() >= 0
                && editedTeam.getMaxNPCsPerPlayer() <= RecruitsServerConfig.MaxRecruitsForPlayer.get()
                && editedTeam.getUnitColor() >= 0
                && editedTeam.getUnitColor() <= FactionNetworkAuthority.MAX_UNIT_COLOR_INDEX
                && ChatFormatting.getById(editedTeam.getTeamColor()) != null;
    }

    private static int getEditCost(RecruitsFaction currentTeam, RecruitsFaction editedTeam) {
        int cost = 0;
        int changeCost = RecruitsServerConfig.FactionCreationCost.get();
        if (!currentTeam.getTeamLeaderUUID().equals(editedTeam.getTeamLeaderUUID())) cost += changeCost;
        if (!currentTeam.getTeamDisplayName().equals(editedTeam.getTeamDisplayName())) cost += changeCost;
        if (!currentTeam.getBanner().equals(editedTeam.getBanner())) cost += changeCost;
        if (currentTeam.getUnitColor() != editedTeam.getUnitColor()) cost += changeCost;
        if (currentTeam.getTeamColor() != editedTeam.getTeamColor()) cost += changeCost;
        if (currentTeam.getMaxNPCsPerPlayer() != editedTeam.getMaxNPCsPerPlayer()) cost += changeCost;
        return cost;
    }

    @Override
    public MessageSaveTeamSettings fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.stringID = buf.readUtf();
        this.cost = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeUtf(this.stringID);
        buf.writeInt(this.cost);
    }
}
