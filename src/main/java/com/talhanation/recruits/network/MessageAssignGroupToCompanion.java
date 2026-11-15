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
import net.minecraft.world.entity.Entity;
import javax.annotation.Nullable;

public class MessageAssignGroupToCompanion implements Message<MessageAssignGroupToCompanion>, CompanionOrder<AbstractLeaderEntity> {

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
        CompanionOrders.handle(context.getSender(), this);
    }

    @Override
    public UUID getCompanion() {
        return this.companionUUID;
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
        if (!(companionEntity.level() instanceof ServerLevel serverLevel)) return;
        // Only your own leader, with your own group, and only your own recruits.
        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(companionEntity.getGroup());
        if (!CompanionOrders.canManageGroup(companionEntity, sender, group)) return;

        // Given by the player himself: the recruits around him, as before. Delivered by a Messenger: the recruits
        // around the leader, the player may be at the other end of the map.
        Entity center = sender != null ? sender : companionEntity;
        List<LivingEntity> list = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                center.getBoundingBox().inflate(100)
        );

        list.removeIf(living -> !(living instanceof AbstractRecruitEntity recruit)
                || (recruit.getGroup() == null || !recruit.getGroup().equals(group.getUUID()))
                || recruit.isGarrison()
                || recruit.getUUID().equals(this.companionUUID));

        for (LivingEntity living : list) {
            if(living instanceof AbstractRecruitEntity recruit) ICompanion.assignToLeaderCompanion(companionEntity, recruit);
        }

        companionEntity.army = new NPCArmy(serverLevel, list, null);
        group.leaderUUID = companionUUID;
        companionEntity.setGroupUUID(group.getUUID());

        ServerPlayer player = CompanionOrders.playerFor(companionEntity, sender);
        if (player != null) RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
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