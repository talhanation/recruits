package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.world.RecruitsGroup;
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
        if (serverPLayer == null || updatedGroup == null) return;
        if (!serverPLayer.getUUID().equals(updatedGroup.getPlayerUUID())) return;
        RecruitsGroup existingGroup = RecruitEvents.recruitsGroupsManager.getGroup(updatedGroup.getUUID());
        if (existingGroup != null && !serverPLayer.getUUID().equals(existingGroup.getPlayerUUID())) return;

        RecruitEvents.recruitsGroupsManager.addOrUpdateGroup((ServerLevel) serverPLayer.getCommandSenderWorld(), serverPLayer, updatedGroup);
    }
    public MessageUpdateGroup fromBytes(FriendlyByteBuf buf) {
        this.groupNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(groupNBT);
    }
}
