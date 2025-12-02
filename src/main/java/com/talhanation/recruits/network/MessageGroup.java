package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageGroup implements Message<MessageGroup> {

    private CompoundTag nbt;
    private UUID recruitUUID;

    public MessageGroup() {
    }

    public MessageGroup(RecruitsGroup group, UUID recruitUUID) {
        this.nbt = group.toNBT();
        this.recruitUUID = recruitUUID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.getUUID().equals(this.recruitUUID)
        ).forEach((recruit) -> this.setGroup(recruit, player, RecruitsGroup.fromNBT(this.nbt)));
    }

    public void setGroup(AbstractRecruitEntity recruit, ServerPlayer player, RecruitsGroup group){
        RecruitsGroup oldGroup = RecruitEvents.recruitsGroupsManager.getGroup(recruit.getGroup());
        RecruitsGroup newGroup = RecruitEvents.recruitsGroupsManager.getGroup(group.getUUID());
        if(oldGroup != null && newGroup != null && oldGroup.getUUID().equals(newGroup.getUUID())) return;

        if(oldGroup != null)oldGroup.decreaseSize();
        if(newGroup != null)newGroup.increaseSize();

        RecruitEvents.recruitsGroupsManager.broadCastGroupsToPlayer(player);

        recruit.setGroup(newGroup);
    }

    public MessageGroup fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.recruitUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
        buf.writeUUID(recruitUUID);
    }
}