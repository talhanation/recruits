package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageRecruitGui implements Message<MessageRecruitGui> {

    private UUID uuid;
    private UUID recruit;


    public MessageRecruitGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageRecruitGui(PlayerEntity player, UUID recruit) {
        this.uuid = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (!context.getSender().getUUID().equals(uuid)) {
            return;
        }

        ServerPlayerEntity player = context.getSender();
        player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(recruit -> recruit.openGUI(player));
    }

    @Override
    public MessageRecruitGui fromBytes(PacketBuffer buf) {
        this.uuid = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(PacketBuffer buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(recruit);
    }

}