package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class MessageRemoveAssignedGroupFromCompanion implements Message<MessageRemoveAssignedGroupFromCompanion> {

    public static final CustomPacketPayload.Type<MessageRemoveAssignedGroupFromCompanion> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageremoveassignedgroupfromcompanion"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = ((ServerPlayer) context.player());
        serverPlayer.serverLevel().getEntitiesOfClass(AbstractLeaderEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(100D),
                (leader) -> leader.getUUID().equals(this.companion)
        ).forEach((companionEntity) -> {
            if(companionEntity == null) return;

            RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(companionEntity.getGroup());
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

            de.maxhenkel.corelib.net.NetUtils.sendTo(serverPlayer, new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getArmySize()));
        });
    }

    public MessageRemoveAssignedGroupFromCompanion fromBytes(RegistryFriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.companion = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.companion);
    }

    @Override
    public CustomPacketPayload.Type<MessageRemoveAssignedGroupFromCompanion> type() {
        return TYPE;
    }
}