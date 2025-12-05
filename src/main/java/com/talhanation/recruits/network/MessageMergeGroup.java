package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageMergeGroup implements Message<MessageMergeGroup> {

    private UUID groupUUID;
    private UUID mergeUUID;

    public MessageMergeGroup() {
    }

    public MessageMergeGroup(UUID mergeUUID, UUID groupUUID) {
        this.mergeUUID = mergeUUID;
        this.groupUUID = groupUUID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup groupToMerge = RecruitEvents.recruitsGroupsManager.getGroup(mergeUUID);
        RecruitsGroup baseGroup = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToMerge == null || baseGroup == null) return;

        RecruitEvents.recruitsGroupsManager.mergeGroups(groupToMerge, baseGroup, player.serverLevel());
    }

    public MessageMergeGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        this.mergeUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
        buf.writeUUID(mergeUUID);
    }
}