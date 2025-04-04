package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.util.NPCArmy;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
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
        ServerLevel serverLevel =  context.getSender().getServer().overworld();
        int group = -1;
        AbstractLeaderEntity companionEntity = null;

        List<LivingEntity> list = Objects.requireNonNull(serverLevel.getEntitiesOfClass(LivingEntity.class, Objects.requireNonNull(context.getSender())
                .getBoundingBox().inflate(100D)));

        for (LivingEntity companion : list){
            if(companion.getUUID().equals(this.companionUUID)){
                group = ((AbstractLeaderEntity)companion).getGroup();
                companionEntity = (AbstractLeaderEntity) companion;
                break;
            }
        }
        if(companionEntity == null) return;

        int finalGroup = group;
        list.removeIf(living -> !(living instanceof AbstractRecruitEntity recruit)
                || (!recruit.isEffectedByCommand(ownerUUID, finalGroup))
                || recruit.getGroup() != finalGroup
                || recruit.getUUID().equals(this.companionUUID));

        for (LivingEntity living : list) {
            if(living instanceof AbstractRecruitEntity recruit) ICompanion.assignToLeaderCompanion(companionEntity, recruit);
        }
        companionEntity.army = new NPCArmy(serverLevel, list, null);

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(context::getSender), new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getArmySize()));
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