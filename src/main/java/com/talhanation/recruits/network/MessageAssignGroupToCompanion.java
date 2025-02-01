package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;

public class MessageAssignGroupToCompanion implements Message<MessageAssignGroupToCompanion> {

    private UUID owner;
    private UUID companion;

    public MessageAssignGroupToCompanion(){
    }

    public MessageAssignGroupToCompanion(UUID owner, UUID companion) {
        this.owner = owner;
        this.companion = companion;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractRecruitEntity> list = Objects.requireNonNull(
                context.getSender()
        ).getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                Objects.requireNonNull(context.getSender()).getBoundingBox().inflate(100D)
        );

        int group = -1;
        AbstractLeaderEntity companionEntity = null;
        for (AbstractRecruitEntity companion : list){
            if(companion.getUUID().equals(this.companion)){
                group = companion.getGroup();
                companionEntity = (AbstractLeaderEntity) companion;
                break;
            }
        }
        Objects.requireNonNull(companionEntity).RECRUITS_IN_COMMAND = new Stack<>();
        for (AbstractRecruitEntity recruit : list) {
            UUID recruitOwner = recruit.getOwnerUUID();
            if (recruitOwner != null && recruitOwner.equals(owner) && recruit.getGroup() == group && !recruit.getUUID().equals(this.companion))
                ICompanion.assignToLeaderCompanion(companionEntity, recruit);
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(context::getSender), new MessageToClientUpdateLeaderScreen(companionEntity.WAYPOINTS, companionEntity.WAYPOINT_ITEMS, companionEntity.getRecruitsInCommand().size()));
    }
    public MessageAssignGroupToCompanion fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.companion = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.companion);
    }

}