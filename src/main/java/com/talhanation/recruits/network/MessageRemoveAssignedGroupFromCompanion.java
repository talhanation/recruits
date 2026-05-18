package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;
import com.talhanation.recruits.network.compat.RecruitsPacketDistributor;

import java.util.*;

public class MessageRemoveAssignedGroupFromCompanion implements RecruitsMessage<MessageRemoveAssignedGroupFromCompanion> {

    private UUID owner;
    private UUID companion;

    public MessageRemoveAssignedGroupFromCompanion() {
    }

    public MessageRemoveAssignedGroupFromCompanion(UUID owner, UUID companion) {
        this.owner = owner;
        this.companion = companion;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer serverPlayer = context.getSender();
        if (serverPlayer == null || !serverPlayer.getUUID().equals(this.owner)) return;
        RecruitCommandTargetResolver.resolveOwnedLeader(serverPlayer, this.companion, 100D).ifPresent((companionEntity) -> {
            if(companionEntity == null) return;

            RecruitsGroup group = RecruitCommandAuthority.ownedGroup(serverPlayer, companionEntity.getGroup());
            if(group == null) return;
            group.leaderUUID = null;
            companionEntity.setGroupUUID(group.getUUID());


            if(companionEntity.getArmySize() > 0){
                RecruitCommanderUtil.setRecruitsListen(companionEntity.army.getAllRecruitUnits(), true);
                RecruitCommanderUtil.setRecruitsFollow(companionEntity.army.getAllRecruitUnits(), null);
                RecruitCommanderUtil.setRecruitsHoldPos(companionEntity.army.getAllRecruitUnits());
                RecruitCommanderUtil.setRecruitsMoveSpeed(companionEntity.army.getAllRecruitUnits(), 1F);
            }

            companionEntity.army = null;
            RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(serverPlayer);

            Main.SIMPLE_CHANNEL.send(RecruitsPacketDistributor.PLAYER.with(context::getSender), new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getArmySize()));
        });
    }

    public MessageRemoveAssignedGroupFromCompanion fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.companion = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.companion);
    }
}
