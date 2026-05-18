package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.compat.smallships.SmallShips;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMountEntity implements RecruitsMessage<MessageMountEntity> {

    private UUID uuid;
    private UUID target;
    private UUID group;

    public MessageMountEntity() {
    }

    public MessageMountEntity(UUID uuid, UUID target, UUID group) {
        this.uuid = uuid;
        this.target = target;
        this.group = group;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        List<Entity> entityList = player.getCommandSenderWorld().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(100),
                (mount) -> mount.getUUID().equals(target) && RecruitsServerConfig.MountWhiteList.get().contains(mount.getEncodeId())
                        || SmallShips.isSmallShip(mount)
                        || SiegeWeapon.isSiegeWeapon(mount)
        );
        if (entityList.isEmpty()) return;

        RecruitCommandTargetResolver.resolveGroupTargets(player, this.uuid, this.group, 100D)
                .forEach((recruit) -> CommandEvents.onMountButton(player.getUUID(), recruit, target, group));
    }

    public MessageMountEntity fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeUUID(group);
    }
}
