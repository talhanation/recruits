package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.*;
public class MessageMountEntityGui implements RecruitsMessage<MessageMountEntityGui> {
    private UUID recruit;
    private boolean back;

    public MessageMountEntityGui() {
    }

    public MessageMountEntityGui(UUID recruit, boolean back) {
        this.recruit = recruit;
        this.back = back;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @SuppressWarnings({"all"})
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());

        RecruitCommandTargetResolver.resolveOwnedRecruit(player, this.recruit, 32.0D)
                .ifPresent(recruit -> this.mount(player, recruit));
    }

    @SuppressWarnings({"all"})
    private void mount(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (this.back && recruit.getMountUUID() != null) {
            recruit.shouldMount(true, recruit.getMountUUID());
        } else if (recruit.getVehicle() == null) {
            List<Entity> list = recruit.getCommandSenderWorld().getEntitiesOfClass(
                    Entity.class,
                    recruit.getBoundingBox().inflate(8),
                    (mount) -> recruit.canMountEntity(mount)
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
                player.sendSystemMessage(TEXT_NO_MOUNT(recruit.getName().getString()));
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
