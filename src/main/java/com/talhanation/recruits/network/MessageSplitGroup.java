package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageSplitGroup implements Message<MessageSplitGroup> {

    private UUID groupUUID;

    public MessageSplitGroup() {
    }

    public MessageSplitGroup(UUID groupUUID) {
        this.groupUUID = groupUUID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        RecruitsGroup groupToSplit = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(groupToSplit == null) return;

        RecruitEvents.recruitsGroupsManager.splitGroup(groupToSplit);

        RecruitEvents.serverSideRecruitGroup(player.serverLevel());

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);
    }

    public MessageSplitGroup fromBytes(FriendlyByteBuf buf) {
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(groupUUID);
    }
}