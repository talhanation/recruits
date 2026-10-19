package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsGroupsManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageUpdateGroup implements Message<MessageUpdateGroup> {

    private CompoundTag groupNBT;

    public MessageUpdateGroup(){

    }

    public MessageUpdateGroup(RecruitsGroup group) {
        this.groupNBT = group.toNBT();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        RecruitsGroup updatedGroup = RecruitsGroup.fromNBT(this.groupNBT);
        ServerPlayer serverPLayer = context.getSender();
        if (updatedGroup == null || serverPLayer == null) return;

        // The client may only edit (name, image, removed) its OWN groups. Owner, leader and
        // disband state always come from the server copy. Before: anyone could overwrite,
        // delete or take over any group by UUID.
        RecruitsGroup existing = RecruitEvents.recruitsGroupsManager.getGroup(updatedGroup.getUUID());
        if (existing != null) {
            if (!NetworkSecurity.canManageGroup(serverPLayer, existing)) return;
            updatedGroup.setPlayer(new RecruitsPlayerInfo(existing.getPlayerUUID(), existing.getPlayerName()));
            updatedGroup.leaderUUID = existing.leaderUUID;
            updatedGroup.setDisbandContext(existing.disbandContext);
        } else {
            if (updatedGroup.removed) return;
            if (RecruitEvents.recruitsGroupsManager.getPlayerGroups(serverPLayer).size() >= MAX_GROUPS_PER_PLAYER) return;
            updatedGroup.setPlayer(serverPLayer);
            updatedGroup.leaderUUID = null;
            updatedGroup.setDisbandContext(new RecruitsGroup.DisbandContext(false, false, false));
        }
        updatedGroup.setName(NetworkSecurity.cleanText(updatedGroup.getName(), 32));

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup((ServerLevel) serverPLayer.getCommandSenderWorld(), serverPLayer, updatedGroup);
    }
    private static final int MAX_GROUPS_PER_PLAYER = 100;

    public MessageUpdateGroup fromBytes(FriendlyByteBuf buf) {
        this.groupNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(groupNBT);
    }
}
