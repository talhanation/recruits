package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.compat.smallships.SmallShips;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMountEntity implements Message<MessageMountEntity> {

    public static final CustomPacketPayload.Type<MessageMountEntity> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagemountentity"));
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

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        List<Entity> entityList = player.getCommandSenderWorld().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(100),
                (mount) -> mount.getUUID().equals(target) && RecruitsServerConfig.MountWhiteList.get().contains(mount.getEncodeId())
                        || SmallShips.isSmallShip(mount)
                        || SiegeWeapon.isSiegeWeapon(mount)
        );
        if (entityList.isEmpty()) return;

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100),
                (recruit) -> recruit.isEffectedByCommand(uuid, group)
        ).forEach((recruit) -> CommandEvents.onMountButton(uuid, recruit, target, group));
    }

    public MessageMountEntity fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeUUID(group);
    }

    @Override
    public CustomPacketPayload.Type<MessageMountEntity> type() {
        return TYPE;
    }
}