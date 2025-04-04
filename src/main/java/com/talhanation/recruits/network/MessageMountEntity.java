package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMountEntity implements Message<MessageMountEntity> {

    private UUID uuid;
    private UUID target;
    private int group;

    public MessageMountEntity() {
    }

    public MessageMountEntity(UUID uuid, UUID target, int group) {
        this.uuid = uuid;
        this.target = target;
        this.group = group;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        List<Entity> entityList = player.getCommandSenderWorld().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(100),
                (mount) -> mount.getUUID().equals(target) && RecruitsServerConfig.MountWhiteList.get().contains(mount.getEncodeId())
        );
        if (entityList.isEmpty()) return;

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.isEffectedByCommand(uuid, group)
        ).forEach((recruit) -> CommandEvents.onMountButton(uuid, recruit, target, group));
    }

    public MessageMountEntity fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeInt(group);
    }
}