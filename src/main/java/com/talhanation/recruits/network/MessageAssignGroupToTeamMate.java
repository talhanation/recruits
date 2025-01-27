package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssignGroupToTeamMate implements Message<MessageAssignGroupToTeamMate> {

    private UUID owner;
    private UUID recruit;

    public MessageAssignGroupToTeamMate(){
    }

    public MessageAssignGroupToTeamMate(UUID owner, UUID recruit) {
        this.owner = owner;
        this.recruit = recruit;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        List<AbstractRecruitEntity> list = serverPlayer.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                context.getSender().getBoundingBox().inflate(100D),
                (recruit) -> recruit.getOwnerUUID() != null && recruit.getOwnerUUID().equals(owner)
        );
        int group = -1;

        for (AbstractRecruitEntity recruit1 : list){
            if(recruit1.getUUID().equals(recruit)){
                group = recruit1.getGroup();
                break;
            }
        }

        for (AbstractRecruitEntity recruit : list) {
            if (recruit.getGroup() == group)
                TeamEvents.assignToTeamMate(serverPlayer, recruit);
        }
    }
    public MessageAssignGroupToTeamMate fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.recruit);
    }

}