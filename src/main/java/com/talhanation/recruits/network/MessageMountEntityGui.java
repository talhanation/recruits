package com.talhanation.recruits.network;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Function;

public class MessageMountEntityGui implements Message<MessageMountEntityGui> {
    private UUID recruit;
    private boolean back;

    public MessageMountEntityGui() {
    }

    public MessageMountEntityGui(UUID recruit, boolean back) {
        this.recruit = recruit;
        this.back = back;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @SuppressWarnings({"all"})
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(32.0D),
                v -> v.getUUID().equals(this.recruit) && v.isAlive()
        ).forEach(this::mount);
    }

    @SuppressWarnings({"all"})
    private void mount(AbstractRecruitEntity recruit) {
        if (this.back && recruit.getMountUUID() != null) {
            recruit.shouldMount(true, recruit.getMountUUID());
        } else if (recruit.getVehicle() == null) {
            List<Entity> list = recruit.getCommandSenderWorld().getEntitiesOfClass(
                    Entity.class,
                    recruit.getBoundingBox().inflate(8),
                    (mount) -> !(mount instanceof AbstractHorse horse &&
                            horse.hasControllingPassenger()) &&
                            RecruitsServerConfig.MountWhiteList.get().contains(mount.getEncodeId())
            );

            double d0 = -1.0D;
            Entity horse = null;

            for (Entity entity : list) {
                double d1 = entity.distanceToSqr(recruit);
                if (d0 == -1.0D || d1 < d0) {
                    horse = entity;
                    d0 = d1;
                }
            }

            if (horse == null) {
                recruit.getOwner().sendSystemMessage(TEXT_NO_MOUNT(recruit.getName().getString()));
                return;
            }

            recruit.shouldMount(true, horse.getUUID());
        }
    }

    public MessageMountEntityGui fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.back = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.back);
    }

    private static MutableComponent TEXT_NO_MOUNT(String name) {
        return Component.translatable("chat.recruits.text.noMount", name);
    }
}
