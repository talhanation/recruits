package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.util.NPCArmy;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageAssignGroupToCompanion implements Message<MessageAssignGroupToCompanion> {

    private UUID ownerUUID;
    private UUID companionUUID;
    public MessageAssignGroupToCompanion(){
    }

    public MessageAssignGroupToCompanion(UUID owner, UUID companionUUID) {
        this.ownerUUID = owner;
        this.companionUUID = companionUUID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer =  context.getSender();
        ServerLevel serverLevel =  serverPlayer.serverLevel();

        AbstractLeaderEntity companionEntity = null;

        List<LivingEntity> list = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                serverPlayer.getBoundingBox().inflate(100)
        );

        for (LivingEntity companion : list){
            if(companion.getUUID().equals(this.companionUUID)){
                companionEntity = (AbstractLeaderEntity) companion;
                break;
            }
        }
        if(companionEntity == null) return;


        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(companionEntity.getGroup());
        if(group == null) return;

        list.removeIf(living -> !(living instanceof AbstractRecruitEntity recruit)
                || (recruit.getGroup() == null || !recruit.getGroup().equals(group.getUUID()))
                || recruit.getUUID().equals(this.companionUUID));

        for (LivingEntity living : list) {
            if(living instanceof AbstractRecruitEntity recruit) ICompanion.assignToLeaderCompanion(companionEntity, recruit);
        }
        companionEntity.army = new NPCArmy(serverLevel, list, null);
        group.leaderUUID = companionUUID;
        companionEntity.setGroup(group);

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(serverPlayer);
    }

    public MessageAssignGroupToCompanion fromBytes(FriendlyByteBuf buf) {
        this.ownerUUID = buf.readUUID();
        this.companionUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.ownerUUID);
        buf.writeUUID(this.companionUUID);
    }

}