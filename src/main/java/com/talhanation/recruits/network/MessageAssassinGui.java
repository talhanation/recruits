package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageAssassinGui implements Message<MessageAssassinGui> {

    private UUID uuid;
    private UUID recruit;


    public MessageAssassinGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageAssassinGui(PlayerEntity player, UUID recruit) {
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
        player.level.getEntitiesOfClass(AssassinLeaderEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.recruit))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(recruit -> recruit.openGUI(player));
    }

    @Override
    public MessageAssassinGui fromBytes(PacketBuffer buf) {
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