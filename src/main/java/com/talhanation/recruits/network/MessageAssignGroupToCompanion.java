package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.util.NPCArmy;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.UUID;

public class MessageAssignGroupToCompanion implements Message<MessageAssignGroupToCompanion> {

    public static final CustomPacketPayload.Type<MessageAssignGroupToCompanion> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassigngrouptocompanion"));
    private UUID ownerUUID;
    private UUID companionUUID;
    public MessageAssignGroupToCompanion(){
    }

    public MessageAssignGroupToCompanion(UUID owner, UUID companionUUID) {
        this.ownerUUID = owner;
        this.companionUUID = companionUUID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer =  ((ServerPlayer) context.player());
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
        companionEntity.setGroupUUID(group.getUUID());

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(serverPlayer);
    }

    public MessageAssignGroupToCompanion fromBytes(RegistryFriendlyByteBuf buf) {
        this.ownerUUID = buf.readUUID();
        this.companionUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.ownerUUID);
        buf.writeUUID(this.companionUUID);
    }


    @Override
    public CustomPacketPayload.Type<MessageAssignGroupToCompanion> type() {
        return TYPE;
    }
}