package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageDisbandGroup implements Message<MessageDisbandGroup> {

    private UUID owner;
    private UUID groupUUID;
    private boolean keepTeam;

    public MessageDisbandGroup() {
    }

    public MessageDisbandGroup(UUID owner, UUID groupUUID, boolean keepTeam) {
        this.owner = owner;
        this.groupUUID = groupUUID;
        this.keepTeam = keepTeam;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if(group == null) return;

        group.setDisbandContext(new RecruitsGroup.DisbandContext(true, keepTeam, true));

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);

        List<AbstractRecruitEntity> list = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100D)
        );

        for(AbstractRecruitEntity recruit : list){
            recruit.needsGroupUpdate = true;
        }
    }

    public MessageDisbandGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.groupUUID = buf.readUUID();
        this.keepTeam = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeUUID(groupUUID);
        buf.writeBoolean(keepTeam);
    }
}