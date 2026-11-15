package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import javax.annotation.Nullable;

public class MessageRemoveAssignedGroupFromCompanion implements Message<MessageRemoveAssignedGroupFromCompanion>, CompanionOrder<AbstractLeaderEntity> {

    private UUID owner;
    private UUID companion;

    public MessageRemoveAssignedGroupFromCompanion() {
    }

    public MessageRemoveAssignedGroupFromCompanion(UUID owner, UUID companion) {
        this.owner = owner;
        this.companion = companion;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.companion;
    }

    @Override
    public Class<AbstractLeaderEntity> getCompanionType() {
        return AbstractLeaderEntity.class;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public void applyTo(AbstractLeaderEntity companionEntity, @Nullable ServerPlayer sender) {
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

        ServerPlayer player = CompanionOrders.playerFor(companionEntity, sender);
        if (player == null) return;
        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getArmySize()));
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